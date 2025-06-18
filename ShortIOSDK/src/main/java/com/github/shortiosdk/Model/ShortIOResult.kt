package com.github.shortiosdk

sealed class ShortIOResult {
    data class Success(val data: ShortIOResponseModel) : ShortIOResult()
    data class Error(val data: ShortIOErrorModel) : ShortIOResult()
}

sealed class StringOrInt {
    data class Str(val value: String) : StringOrInt()
    data class IntVal(val value: Int) : StringOrInt()
}
