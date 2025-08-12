package com.github.shortiosdk.Helpers

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL


fun extractClidFromUrl(urlString: String): String? {
    return try {
        val uri = Uri.parse(urlString)
        uri.getQueryParameter("clid")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun HandleClick(uriString: String): String? = withContext(Dispatchers.IO) {
    try {
        val urlString = when {
            uriString.contains("utm_medium=android", ignoreCase = true) -> uriString
            uriString.contains("?") -> "$uriString&utm_medium=android"
            else -> "$uriString?utm_medium=android"
        }

        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .build()

        val request = Request.Builder()
            .url(URL(urlString))
            .head()
            .build()

        client.newCall(request).execute().use { response ->

            response.header("Location")
        }
    } catch (e: Exception) {
        println("Network error: ${e.localizedMessage}")
        null
    }
}
