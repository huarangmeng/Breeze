#include <jni.h>
#include <llama.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace {

JavaVM * g_vm = nullptr;
std::once_flag g_backend_once;

struct ModelHandle {
    llama_model * model = nullptr;
    int context_window = 0;
    std::mutex mutex;
};

struct GenerationHandle {
    std::atomic_bool cancelled{false};
    jobject callback = nullptr;
    jmethodID on_token = nullptr;
    jmethodID on_complete = nullptr;
    jmethodID on_error = nullptr;
    std::thread worker;
};

struct AttachedEnv {
    JNIEnv * env = nullptr;
    bool attached = false;
};

std::string to_string(JNIEnv * env, jstring value) {
    if (value == nullptr) {
        return {};
    }
    const char * chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        throw std::runtime_error("Failed to read Java string");
    }
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

void ensure_backend() {
    std::call_once(g_backend_once, [] {
        ggml_backend_load_all();
    });
}

AttachedEnv attach_env() {
    JNIEnv * env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK) {
        return { env, false };
    }
    if (g_vm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr) != JNI_OK) {
        return {};
    }
    return { env, true };
}

void detach_env(const AttachedEnv & attached_env) {
    if (attached_env.attached && g_vm != nullptr) {
        g_vm->DetachCurrentThread();
    }
}

void emit_string(JNIEnv * env, jobject callback, jmethodID method, const std::string & value) {
    jstring text = env->NewStringUTF(value.c_str());
    if (text == nullptr) {
        return;
    }
    env->CallVoidMethod(callback, method, text);
    env->DeleteLocalRef(text);
}

void emit_error(JNIEnv * env, GenerationHandle * generation, const std::string & message) {
    if (env != nullptr && generation != nullptr && generation->callback != nullptr && generation->on_error != nullptr) {
        emit_string(env, generation->callback, generation->on_error, message);
    }
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

std::vector<llama_token> tokenize_prompt(const llama_vocab * vocab, const std::string & prompt) {
    const int count = -llama_tokenize(vocab, prompt.c_str(), static_cast<int32_t>(prompt.size()), nullptr, 0, true, true);
    if (count <= 0) {
        throw std::runtime_error("Failed to tokenize prompt");
    }
    std::vector<llama_token> tokens(static_cast<size_t>(count));
    const int actual = llama_tokenize(
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

void run_generation(
    GenerationHandle * generation,
    ModelHandle * model_handle,
    std::string prompt,
    float temperature,
    float top_p,
    int max_tokens,
    int context_window
) {
    AttachedEnv attached_env = attach_env();
    JNIEnv * env = attached_env.env;
    if (env == nullptr) {
        return;
    }

    std::unique_ptr<llama_context, decltype(&llama_free)> ctx(nullptr, llama_free);
    std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(nullptr, llama_sampler_free);

    try {
        std::lock_guard<std::mutex> lock(model_handle->mutex);
        const llama_vocab * vocab = llama_model_get_vocab(model_handle->model);
        std::vector<llama_token> prompt_tokens = tokenize_prompt(vocab, prompt);
        if (static_cast<int>(prompt_tokens.size()) >= context_window) {
            throw std::runtime_error("Prompt is longer than the configured context window");
        }

        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.n_ctx = static_cast<uint32_t>(context_window);
        ctx_params.n_batch = static_cast<uint32_t>(std::min<int>(context_window, 512));
        ctx_params.n_threads = std::max<int>(1, static_cast<int>(std::thread::hardware_concurrency()));
        ctx_params.n_threads_batch = ctx_params.n_threads;
        ctx_params.no_perf = true;

        ctx.reset(llama_init_from_model(model_handle->model, ctx_params));
        if (!ctx) {
            throw std::runtime_error("Failed to create llama context");
        }
        sampler.reset(create_sampler(temperature, top_p));

        llama_batch batch = llama_batch_get_one(prompt_tokens.data(), static_cast<int32_t>(prompt_tokens.size()));
        if (llama_decode(ctx.get(), batch) != 0) {
            throw std::runtime_error("Failed to decode prompt");
        }

        int generated = 0;
        while (!generation->cancelled.load() && generated < max_tokens) {
            llama_token token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
            if (llama_vocab_is_eog(vocab, token)) {
                break;
            }

            std::string piece = token_to_piece(vocab, token);
            emit_string(env, generation->callback, generation->on_token, piece);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                break;
            }

            batch = llama_batch_get_one(&token, 1);
            if (llama_decode(ctx.get(), batch) != 0) {
                throw std::runtime_error("Failed to decode generated token");
            }
            generated += 1;
        }

        if (!generation->cancelled.load()) {
            env->CallVoidMethod(generation->callback, generation->on_complete);
        }
    } catch (const std::exception & ex) {
        emit_error(env, generation, ex.what());
    } catch (...) {
        emit_error(env, generation, "Unknown llama.cpp generation error");
    }
    detach_env(attached_env);
}

ModelHandle * as_model(jlong handle) {
    auto * model = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(handle));
    if (model == nullptr || model->model == nullptr) {
        throw std::runtime_error("Invalid llama model handle");
    }
    return model;
}

GenerationHandle * as_generation(jlong handle) {
    return reinterpret_cast<GenerationHandle *>(static_cast<intptr_t>(handle));
}

} // namespace

extern "C" jint JNI_OnLoad(JavaVM * vm, void *) {
    g_vm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_hrm_breeze_runtime_llama_BreezeLlamaNativeBridge_nativeLoadModel(
    JNIEnv * env,
    jobject,
    jstring model_path,
    jint context_window
) {
    try {
        ensure_backend();
        const std::string path = to_string(env, model_path);
        llama_model_params model_params = llama_model_default_params();
        model_params.use_mmap = true;

        llama_model * model = llama_model_load_from_file(path.c_str(), model_params);
        if (model == nullptr) {
            throw std::runtime_error("Failed to load GGUF model: " + path);
        }

        auto * handle = new ModelHandle();
        handle->model = model;
        handle->context_window = context_window;
        return static_cast<jlong>(reinterpret_cast<intptr_t>(handle));
    } catch (const std::exception & ex) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), ex.what());
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_hrm_breeze_runtime_llama_BreezeLlamaNativeBridge_nativeUnload(
    JNIEnv *,
    jobject,
    jlong handle
) {
    auto * model_handle = reinterpret_cast<ModelHandle *>(static_cast<intptr_t>(handle));
    if (model_handle != nullptr) {
        {
            std::lock_guard<std::mutex> lock(model_handle->mutex);
            if (model_handle->model != nullptr) {
                llama_model_free(model_handle->model);
                model_handle->model = nullptr;
            }
        }
        delete model_handle;
    }
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_hrm_breeze_runtime_llama_BreezeLlamaNativeBridge_nativeGenerate(
    JNIEnv * env,
    jobject,
    jlong model_handle_value,
    jstring prompt_value,
    jfloat temperature,
    jfloat top_p,
    jint max_tokens,
    jint context_window,
    jobject callback
) {
    try {
        ModelHandle * model_handle = as_model(model_handle_value);
        auto * generation = new GenerationHandle();
        generation->callback = env->NewGlobalRef(callback);
        jclass callback_class = env->GetObjectClass(callback);
        generation->on_token = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
        generation->on_complete = env->GetMethodID(callback_class, "onComplete", "()V");
        generation->on_error = env->GetMethodID(callback_class, "onError", "(Ljava/lang/String;)V");
        env->DeleteLocalRef(callback_class);

        if (generation->callback == nullptr || generation->on_token == nullptr ||
            generation->on_complete == nullptr || generation->on_error == nullptr) {
            if (generation->callback != nullptr) {
                env->DeleteGlobalRef(generation->callback);
            }
            delete generation;
            throw std::runtime_error("Invalid Breeze llama callback");
        }

        const std::string prompt = to_string(env, prompt_value);
        generation->worker = std::thread(
            run_generation,
            generation,
            model_handle,
            prompt,
            static_cast<float>(temperature),
            static_cast<float>(top_p),
            static_cast<int>(max_tokens),
            static_cast<int>(context_window)
        );
        return static_cast<jlong>(reinterpret_cast<intptr_t>(generation));
    } catch (const std::exception & ex) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), ex.what());
        return 0;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_hrm_breeze_runtime_llama_BreezeLlamaNativeBridge_nativeCancel(
    JNIEnv * env,
    jobject,
    jlong handle
) {
    GenerationHandle * generation = as_generation(handle);
    if (generation == nullptr) {
        return;
    }
    generation->cancelled.store(true);
    if (generation->worker.joinable()) {
        generation->worker.join();
    }
    if (generation->callback != nullptr) {
        env->DeleteGlobalRef(generation->callback);
    }
    delete generation;
}
