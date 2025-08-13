package com.github.shortiosdk.Helpers

import android.net.Uri


fun extractClidFromUrl(urlString: String): String? {
    return try {
        val uri = Uri.parse(urlString)
        uri.getQueryParameter("clid")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun removeUtmParams(url: String): String {
    val uri = Uri.parse(url)
    val builder = uri.buildUpon().clearQuery()

    uri.queryParameterNames
        .filter { !it.startsWith("utm_", ignoreCase = true) }
        .forEach { key ->
            uri.getQueryParameters(key)?.forEach { value ->
                builder.appendQueryParameter(key, value)
            }
        }

    return builder.build().toString()
}