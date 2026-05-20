#ifndef BREEZE_LLAMA_APPLE_BRIDGE_H
#define BREEZE_LLAMA_APPLE_BRIDGE_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

int64_t breeze_llama_load_model(const char * model_path, int32_t context_window);
void breeze_llama_unload(int64_t handle);

int64_t breeze_llama_start_generation(
    int64_t model_handle,
    const char * prompt,
    float temperature,
    float top_p,
    int32_t max_tokens,
    int32_t context_window
);

int32_t breeze_llama_next_token(int64_t generation_handle, char ** out_token);
void breeze_llama_cancel(int64_t generation_handle);
void breeze_llama_generation_free(int64_t generation_handle);
void breeze_llama_string_free(char * value);
const char * breeze_llama_last_error(void);

#ifdef __cplusplus
}
#endif

#endif
