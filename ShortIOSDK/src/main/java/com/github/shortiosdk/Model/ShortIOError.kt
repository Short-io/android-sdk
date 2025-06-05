package com.github.shortiosdk

data class ShortIOErrorModel(
    val message: String?,
    val statusCode: Int?,
    val code: String?,
    val success: Boolean?
)