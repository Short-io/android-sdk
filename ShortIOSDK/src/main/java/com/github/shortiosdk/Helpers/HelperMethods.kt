package com.github.shortiosdk.Helpers

import android.net.Uri
import android.util.Log


fun extractClidFromUrl(urlString: String): String? {
    return try {
        val uri = Uri.parse(urlString)
        uri.getQueryParameter("clid")
    } catch (e: Exception) {
        Log.e("ShortioSdk", "could not read clid from $urlString", e)
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