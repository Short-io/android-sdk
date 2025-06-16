package com.github.shortiosdk

data class UrlComponents(
    val scheme: String?,
    val host: String?,
    val path: String?,
    val query: String?,
    val fragment: String?,
    val fullUrl: String
)