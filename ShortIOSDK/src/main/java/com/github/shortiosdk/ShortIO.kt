package com.github.shortiosdk

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.GsonBuilder
import com.github.shortiosdk.Helpers.StringOrIntSerializer
import android.content.Intent
import android.net.Uri
import com.github.shortiosdk.Model.UrlComponents


object ShortioSdk {
    fun shortenUrl(
        apiKey: String,
        parameters: ShortIOParameters
    ): ShortIOResult {
        val gson = GsonBuilder()
            .registerTypeAdapter(StringOrInt::class.java, StringOrIntSerializer())
            .create()
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val jsonBody = gson.toJson(parameters)
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.short.io/links/public")
            .post(body)
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/json")
            .addHeader("authorization", apiKey)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        return if (response.isSuccessful) {
            val model = responseBody?.let { gson.fromJson(it, ShortIOResponseModel::class.java) }
            if (model != null) {
                ShortIOResult.Success(model)
            } else {
                val errorModel = ShortIOErrorModel(
                    message = "Empty or malformed success response",
                    statusCode = response.code,
                    code = "MALFORMED_SUCCESS",
                    success = false
                )
                ShortIOResult.Error(errorModel)
            }
        } else {
            val errorModel = try {
                responseBody?.let {
                    gson.fromJson(it, ShortIOErrorModel::class.java)?.copy(statusCode = response.code)
                } ?: ShortIOErrorModel(
                    message = "Unknown error",
                    statusCode = response.code,
                    code = "UNKNOWN",
                    success = false
                )
            } catch (e: Exception) {
                ShortIOErrorModel(
                    message = "Malformed error response: ${e.localizedMessage}",
                    statusCode = response.code,
                    code = "INVALID_JSON",
                    success = false
                )
            }
            return ShortIOResult.Error(errorModel)
        }
    }
    
    fun handleIntent(intent: Intent): UrlComponents? {
        val uri = intent.data ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host ?: return null
        val cleanPath = uri.path?.removePrefix("/") ?: ""
        val query = uri.encodedQuery
        val fragment = uri.fragment

        val fullUri = Uri.Builder()
            .scheme(scheme)
            .authority(host)
            .encodedPath(cleanPath)
            .encodedQuery(query)
            .fragment(fragment)
            .build()

        return UrlComponents(
            scheme = scheme,
            host = host,
            path = cleanPath,
            query = query,
            fragment = fragment,
            fullUrl = fullUri.toString()
        )
    }
}
