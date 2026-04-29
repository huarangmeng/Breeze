package com.hrm.breeze.core

/**
 * 统一的结果类型。业务层的 suspend/Flow 返回优先用它，避免直接抛异常。
 */
sealed interface BreezeResult<out T> {
    data class Success<T>(val value: T) : BreezeResult<T>
    data class Failure(val error: Throwable) : BreezeResult<Nothing>
}

inline fun <T, R> BreezeResult<T>.map(transform: (T) -> R): BreezeResult<R> = when (this) {
    is BreezeResult.Success -> BreezeResult.Success(transform(value))
    is BreezeResult.Failure -> this
}

inline fun <T> BreezeResult<T>.getOrElse(onFailure: (Throwable) -> T): T = when (this) {
    is BreezeResult.Success -> value
    is BreezeResult.Failure -> onFailure(error)
}
