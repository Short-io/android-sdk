package com.github.shortiosdk

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.GsonBuilder
import android.content.Intent
import com.github.shortiosdk.Helpers.StringOrIntSerializer
import com.github.shortiosdk.Helpers.HandleClick
import android.util.Base64
import android.util.Log
import com.github.shortiosdk.Helpers.extractClidFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec


object ShortioSdk {
    var savedDomain: String? = null
    var storedClid: String = ""

    var apiKey: String = ""
    var isInitialized: Boolean = false

    fun initialize( apiKey: String, domain: String) {
        if (!isInitialized){
            this.apiKey = apiKey
            this.savedDomain = domain
            println("SDK initialized with API key: $apiKey")
            isInitialized = true
        } else{
            Log.d("SDK Initialization","SDK is already Initialzed")
        }
    }

    fun shortenUrl(
        parameters: ShortIOParameters
    ): ShortIOResult {
        val gson = GsonBuilder()
            .registerTypeAdapter(StringOrInt::class.java, StringOrIntSerializer())
            .create()
        if (parameters.domain.isNullOrBlank()) {
            parameters.domain = savedDomain!!
        }
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val jsonBody = gson.toJson(parameters)
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(baseURL)
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
    
    suspend fun handleIntent(intent: Intent): UrlComponents? {
        val uri = intent.data ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host ?: return null

        val shortioClidUrl = withContext(Dispatchers.IO) {
            HandleClick(uri.toString())
        }

        storedClid = shortioClidUrl.let { it?.let { urlString -> extractClidFromUrl(urlString) } ?: "" }

        return UrlComponents(
            scheme = scheme,
            host = host,
            path = uri.path?.removePrefix("/"),
            query = uri.encodedQuery,
            fragment = uri.fragment,
            fullUrl = uri.toString(),
            destinationUrl = shortioClidUrl
        )
    }

    /**
     * It creates a Secure URL
    - parameters: originalURL: String
    - Returns: SecureResult
     */
    fun createSecure(originalURL: String): SecureResult {
        return try {

            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(128)
            val secretKey = keyGenerator.generateKey()

            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
            val urlBytes = originalURL.toByteArray(StandardCharsets.UTF_8)
            val encryptedBytes = cipher.doFinal(urlBytes)

            val encryptedUrlBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val encryptedIvBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val securedOriginalURL = "shortsecure://$encryptedUrlBase64?$encryptedIvBase64"

            val rawKey = secretKey.encoded
            val keyBase64 = Base64.encodeToString(rawKey, Base64.NO_WRAP)
            val securedShortUrl = "#$keyBase64"

            SecureResult(securedOriginalURL, securedShortUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
    - parameters: originalURL: String, clid: String, conversionId: String? = nil
    - conversionId can be 'signup', 'purchase', 'download', etc.
    - Returns: Result Boolean based on status code
     */
    suspend fun trackConversion(
        originalURL: String,
        clid: String? = null,
        conversionId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val originalUri = URI(originalURL)
            val scheme = originalUri.scheme ?: return@withContext false
            val host = originalUri.host ?: return@withContext false

            // Build query params
            val queryParams = mutableListOf<Pair<String, String>>()
            if (!conversionId.isNullOrEmpty()) {
                queryParams.add("c" to conversionId)
            }
            if (!clid.isNullOrEmpty()) {
                queryParams.add("clid" to clid)
            } else{
                queryParams.add("clid" to storedClid)
            }

            val queryString = queryParams.joinToString("&") {
                "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}"
            }

            val finalUrl = "$scheme://$host/.shortio/conversion?$queryString"

            val client = OkHttpClient()

            val request = Request.Builder()
                .url(finalUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
