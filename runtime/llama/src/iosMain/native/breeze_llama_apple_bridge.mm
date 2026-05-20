#include "breeze_llama_apple_bridge.h"

#include <TargetConditionals.h>
#include <ggml-backend.h>
#include <llama.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace {

std::once_flag g_backend_once;
thread_local std::string g_last_error;

struct ModelHandle {
    llama_model * model = nullptr;
    int context_window = 0;
    std::mutex mutex;
};

struct GenerationHandle {
    ModelHandle * owner = nullptr;
    llama_context * context = nullptr;
    llama_sampler * sampler = nullptr;
    const llama_vocab * vocab = nullptr;
    int max_tokens = 0;
    int generated = 0;
    bool done = false;
    bool cancelled = false;
    std::vector<char> pending_utf8;
};

void set_last_error(const std::string & message) {
    g_last_error = message;
}

void clear_last_error() {
    g_last_error.clear();
}

void ensure_backend() {
    std::call_once(g_backend_once, [] {
        llama_backend_init();
        ggml_backend_load_all();
    });
}

ModelHandle * as_model(int64_t handle) {
    auto * model = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(handle));
    if (model == nullptr || model->model == nullptr) {
        throw std::runtime_error("Invalid llama model handle");
    }
    return model;
}

GenerationHandle * as_generation(int64_t handle) {
    auto * generation = reinterpret_cast<GenerationHandle *>(static_cast<intptr_t>(handle));
    if (generation == nullptr || generation->context == nullptr || generation->sampler == nullptr) {
        throw std::runtime_error("Invalid llama generation handle");
    }
    return generation;
}

std::vector<llama_token> tokenize_prompt(const llama_vocab * vocab, const std::string & prompt) {
    const int count = -llama_tokenize(vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), nullptr, 0, true, true);
    if (count <= 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    std::vector<llama_token> tokens(static_cast<size_t>(count));
    const int actual =
        llama_tokenize(
            vocab,
            prompt.c_str(),
            static_cast<int32_t>(prompt.size()),
            tokens.data(),
            static_cast<int32_t>(tokens.size()),
            true,
            true
        );
    if (actual < 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    tokens.resize(static_cast<size_t>(actual));
    return tokens;
}

llama_sampler * create_sampler(float temperature, float top_p) {
    auto params = llama_sampler_chain_default_params();
    params.no_perf = true;
    llama_sampler * sampler = llama_sampler_chain_init(params);
    if (sampler == nullptr) {
        throw std::runtime_error("Failed to create sampler");
    }
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(sampler, llama_sampler_init_top_p(top_p, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }
    return sampler;
}

std::string token_to_piece(const llama_vocab * vocab, llama_token token) {
    std::vector<char> buffer(256);
    int size = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    if (size < 0) {
        buffer.resize(static_cast<size_t>(-size));
        size = llama_token_to_piece(vocab, token, buffer.data(), static_cast<int32_t>(buffer.size()), 0, true);
    }
    if (size < 0) {
        throw std::runtime_error("Failed to convert token to text");
    }
    return std::string(buffer.data(), static_cast<size_t>(size));
}

size_t valid_utf8_prefix_length(const std::vector<char> & bytes) {
    size_t index = 0;
    size_t last_valid = 0;
    while (index < bytes.size()) {
        const unsigned char lead = static_cast<unsigned char>(bytes[index]);
        size_t width = 0;
        if ((lead & 0x80u) == 0) {
            width = 1;
        } else if ((lead & 0xE0u) == 0xC0u) {
            width = 2;
        } else if ((lead & 0xF0u) == 0xE0u) {
            width = 3;
        } else if ((lead & 0xF8u) == 0xF0u) {
            width = 4;
        } else {
            break;
        }
        if (index + width > bytes.size()) {
            break;
        }
        bool valid = true;
        for (size_t i = 1; i < width; ++i) {
            const unsigned char continuation = static_cast<unsigned char>(bytes[index + i]);
            if ((continuation & 0xC0u) != 0x80u) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            break;
        }
        index += width;
        last_valid = index;
    }
    return last_valid;
}

std::string consume_pending_utf8(GenerationHandle * generation, bool flush_all = false) {
    const size_t valid_prefix = valid_utf8_prefix_length(generation->pending_utf8);
    if (valid_prefix == 0) {
        if (flush_all) {
            generation->pending_utf8.clear();
        }
        return {};
    }
    std::string chunk(generation->pending_utf8.data(), valid_prefix);
    generation->pending_utf8.erase(
        generation->pending_utf8.begin(),
        generation->pending_utf8.begin() + static_cast<std::ptrdiff_t>(valid_prefix)
    );
    if (flush_all) {
        generation->pending_utf8.clear();
    }
    return chunk;
}

std::string append_piece_and_extract_chunk(
    GenerationHandle * generation,
    const std::string & piece,
    bool flush_all = false
) {
    generation->pending_utf8.insert(generation->pending_utf8.end(), piece.begin(), piece.end());
    return consume_pending_utf8(generation, flush_all);
}

char * duplicate_string(const std::string & value) {
    char * copy = static_cast<char *>(std::malloc(value.size() + 1));
    if (copy == nullptr) {
        throw std::runtime_error("Failed to allocate token buffer");
    }
    std::memcpy(copy, value.c_str(), value.size() + 1);
    return copy;
}

void free_generation(GenerationHandle * generation) {
    if (generation == nullptr) {
        return;
    }
    if (generation->sampler != nullptr) {
        llama_sampler_free(generation->sampler);
        generation->sampler = nullptr;
    }
    if (generation->context != nullptr) {
        llama_free(generation->context);
        generation->context = nullptr;
    }
    delete generation;
}

} // namespace

extern "C" int64_t breeze_llama_load_model(const char * model_path, int32_t context_window) {
    clear_last_error();
    try {
        ensure_backend();
        if (model_path == nullptr || model_path[0] == '\0') {
            throw std::runtime_error("Missing GGUF model path");
        }

        llama_model_params model_params = llama_model_default_params();
        model_params.use_mmap = true;
#if TARGET_OS_SIMULATOR
        model_params.n_gpu_layers = 0;
#endif
        llama_model * model = llama_model_load_from_file(model_path, model_params);
        if (model == nullptr) {
            throw std::runtime_error(std::string("Failed to load GGUF model: ") + model_path);
        }

        auto * handle = new ModelHandle();
        handle->model = model;
        handle->context_window = static_cast<int>(context_window);
        return static_cast<int64_t>(reinterpret_cast<intptr_t>(handle));
    } catch (const std::exception & ex) {
        set_last_error(ex.what());
        return 0;
    }
}

extern "C" void breeze_llama_unload(int64_t handle) {
    auto * model_handle = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(handle));
    if (model_handle == nullptr) {
        return;
    }
    std::lock_guard<std::mutex> lock(model_handle->mutex);
    if (model_handle->model != nullptr) {
        llama_model_free(model_handle->model);
        model_handle->model = nullptr;
    }
    delete model_handle;
}

extern "C" int64_t breeze_llama_start_generation(
    int64_t model_handle_value,
    const char * prompt_value,
    float temperature,
    float top_p,
    int32_t max_tokens,
    int32_t context_window
) {
    clear_last_error();
    try {
        ModelHandle * model_handle = as_model(model_handle_value);
        if (prompt_value == nullptr) {
            throw std::runtime_error("Missing prompt");
        }

        auto * generation = new GenerationHandle();
        generation->owner = model_handle;
        generation->max_tokens = std::max<int>(1, static_cast<int>(max_tokens));

        std::lock_guard<std::mutex> lock(model_handle->mutex);
        const llama_vocab * vocab = llama_model_get_vocab(model_handle->model);
        std::vector<llama_token> prompt_tokens = tokenize_prompt(vocab, std::string(prompt_value));

        const uint32_t model_context_window = static_cast<uint32_t>(std::max(1, llama_model_n_ctx_train(model_handle->model)));
        const uint32_t requested_context_window = std::min(
            static_cast<uint32_t>(std::max<int32_t>(1, context_window)),
            model_context_window
        );
        if (static_cast<uint32_t>(prompt_tokens.size()) >= requested_context_window) {
            throw std::runtime_error("Prompt is longer than the configured context window");
        }

        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = requested_context_window;
        ctx_params.n_batch = std::min<uint32_t>(
            requested_context_window,
            static_cast<uint32_t>(std::max<size_t>(1, prompt_tokens.size()))
        );
        ctx_params.n_ubatch = ctx_params.n_batch;
        ctx_params.n_threads = std::max<int>(1, static_cast<int>(std::thread::hardware_concurrency()));
        ctx_params.n_threads_batch = ctx_params.n_threads;
        ctx_params.no_perf = true;

        generation->context = llama_init_from_model(model_handle->model, ctx_params);
        if (generation->context == nullptr) {
            throw std::runtime_error("Failed to create llama context");
        }
        generation->sampler = create_sampler(temperature, top_p);
        generation->vocab = vocab;

        const uint32_t actual_context_window = llama_n_ctx(generation->context);
        const uint32_t actual_batch = llama_n_batch(generation->context);
        const uint32_t actual_ubatch = llama_n_ubatch(generation->context);
        if (static_cast<uint32_t>(prompt_tokens.size()) >= actual_context_window) {
            throw std::runtime_error("Prompt is longer than the actual llama context window");
        }
        const uint32_t decode_chunk_size = std::max<uint32_t>(1, std::min(actual_batch, actual_ubatch));

        for (size_t offset = 0; offset < prompt_tokens.size(); offset += decode_chunk_size) {
            const int32_t chunk_size = static_cast<int32_t>(std::min<size_t>(decode_chunk_size, prompt_tokens.size() - offset));
            llama_batch batch = llama_batch_get_one(prompt_tokens.data() + offset, chunk_size);
            if (llama_decode(generation->context, batch) != 0) {
                throw std::runtime_error("Failed to decode prompt");
            }
        }

        return static_cast<int64_t>(reinterpret_cast<intptr_t>(generation));
    } catch (const std::exception & ex) {
        set_last_error(ex.what());
        return 0;
    }
}

extern "C" int32_t breeze_llama_next_token(int64_t generation_handle_value, char ** out_token) {
    clear_last_error();
    if (out_token != nullptr) {
        *out_token = nullptr;
    }
    try {
        GenerationHandle * generation = as_generation(generation_handle_value);
        if (generation->cancelled || generation->done) {
            const std::string tail = consume_pending_utf8(generation, true);
            if (!tail.empty()) {
                if (out_token != nullptr) {
                    *out_token = duplicate_string(tail);
                }
                return 1;
            }
            return 0;
        }

        while (!generation->cancelled && generation->generated < generation->max_tokens) {
            llama_token token = llama_sampler_sample(generation->sampler, generation->context, -1);
            if (llama_vocab_is_eog(generation->vocab, token)) {
                generation->done = true;
                const std::string tail = consume_pending_utf8(generation, true);
                if (!tail.empty()) {
                    if (out_token != nullptr) {
                        *out_token = duplicate_string(tail);
                    }
                    return 1;
                }
                return 0;
            }

            const std::string piece = token_to_piece(generation->vocab, token);
            const std::string chunk = append_piece_and_extract_chunk(generation, piece, false);

            llama_batch batch = llama_batch_get_one(&token, 1);
            if (llama_decode(generation->context, batch) != 0) {
                throw std::runtime_error("Failed to decode generated token");
            }
            generation->generated += 1;

            if (!chunk.empty()) {
                if (out_token != nullptr) {
                    *out_token = duplicate_string(chunk);
                }
                return 1;
            }
        }

        generation->done = true;
        const std::string tail = consume_pending_utf8(generation, true);
        if (!tail.empty()) {
            if (out_token != nullptr) {
                *out_token = duplicate_string(tail);
            }
            return 1;
        }
        return 0;
    } catch (const std::exception & ex) {
        set_last_error(ex.what());
        return -1;
    }
}

extern "C" void breeze_llama_cancel(int64_t generation_handle_value) {
    auto * generation = reinterpret_cast<GenerationHandle *>(static_cast<intptr_t>(generation_handle_value));
    if (generation != nullptr) {
        generation->cancelled = true;
    }
}

extern "C" void breeze_llama_generation_free(int64_t generation_handle_value) {
    free_generation(reinterpret_cast<GenerationHandle *>(static_cast<intptr_t>(generation_handle_value)));
}

extern "C" void breeze_llama_string_free(char * value) {
    if (value != nullptr) {
        std::free(value);
    }
}

extern "C" const char * breeze_llama_last_error(void) {
    return g_last_error.empty() ? nullptr : g_last_error.c_str();
}
