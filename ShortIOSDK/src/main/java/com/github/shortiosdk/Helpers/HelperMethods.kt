package com.github.shortiosdk.Helpers

import okhttp3.OkHttpClient
import okhttp3.Request


fun HandleClick(uri: String): String? {
    val client = OkHttpClient()

    val url = when {
        uri.contains("utm_medium=android", ignoreCase = true) -> uri
        uri.contains("?") -> "$uri&utm_medium=android"
        else -> "$uri?utm_medium=android"
    }

    val request = Request.Builder()
        .url(url)
        .addHeader("accept", "application/json")
        .build()

    return try {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.code.toString()
            } else {
                "Link is not Valid"
            }
        }
    } catch (e: Exception) {
        e.toString()
    }
}