package com.github.shortiosdk.Helpers

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.joinToString


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

fun extractClidFromUrl(urlString: String): String? {
    return try {
        val uri = Uri.parse(urlString)
        uri.getQueryParameter("clid")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun fetchHeadersValue(urlString: String): String? {
    return try {
        val url = URL(urlString)
        val connection = withContext(Dispatchers.IO) {
            url.openConnection() as HttpURLConnection
        }

        connection.requestMethod = "HEAD"
        connection.instanceFollowRedirects = false

        connection.connect()

        println("Response Headers:")
        for ((key, value) in connection.headerFields) {
            if (key != null && value != null) {
                println("  $key: ${value.joinToString()}")
            }
        }

        val redirectedUrl = connection.getHeaderField("Location")

        connection.disconnect()

        redirectedUrl ?: "Not Found"
    } catch (e: Exception) {
        println("Network error: ${e.localizedMessage}")
        null
    }
}

suspend fun trackConversion(
    originalURL: String,
    clid: String?
): Boolean = withContext(Dispatchers.IO) {
    val client = OkHttpClient()

    val baseURL = if (originalURL.endsWith("/")) originalURL.dropLast(1) else originalURL

    val conversionURLString = "$baseURL/.shortio/conversion?clid=$clid"

    try {
        val request = Request.Builder()
            .url(conversionURLString)
            .get()
            .build()

        val response: Response = client.newCall(request).execute()
        response.use { res ->
            if (res.isSuccessful) {
                val bodyString = res.body?.string()
                println("Success! Response body: $bodyString")
            } else {
                println("Failed with status code: ${res.code}")
                println("Response message: ${res.message}")
            }
        }
        response.use { it.isSuccessful }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}