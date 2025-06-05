package com.github.shortiosdk

import com.github.shortiosdk.ShortIOResponseModel

public sealed class ShortIOResult {
    data class Success(val data: ShortIOResponseModel) : ShortIOResult()
    data class Error(val data: ShortIOErrorModel) : ShortIOResult()
}

public sealed class StringOrInt {
    data class Str(val value: String) : StringOrInt()
    data class IntVal(val value: Int) : StringOrInt()
}
