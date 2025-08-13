package com.github.shortiosdk

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.GsonBuilder
import android.content.Intent
import com.github.shortiosdk.Helpers.StringOrIntSerializer
import android.util.Base64
import android.util.Log
import com.github.shortiosdk.Helpers.extractClidFromUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec


object ShortioSdk {
    var apiKey: String = ""
    var savedDestinationUrl: String = ""
    var savedDomain: String? = null
    var storedClid: String = ""
    var isInitialized: Boolean = false

    /**
     * Initialize the SDK through this method to use it.
     * Parameters required:
     * apiKey of type String
     * domain of type String
     */
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

    /**
     * Use this method to check the SDK is already initialized
     */
    fun isSdkInitialized(): Boolean {
        if (!isInitialized) {
            throw IllegalStateException("SDK is not initialized. Please call initialize before using the SDK.")
        }
        return true
    }

    /**
     * Create ShortUrl by using shortenUrl Method
     * Parameters:- It takes ShortIOParameters as parameter which includes originalUrl, domain, clocking, password, title etc.
     */
    fun shortenUrl(
        parameters: ShortIOParameters
    ): ShortIOResult {
        isSdkInitialized()
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

    /**
     * handleIntent() method is used handle the intent and it returns UrlComponents
     * Parameters: intent of type Intent
     * Returns: UrlComponents which includes scheme, host, path, destibnationUrl, etc.
     */
    suspend fun handleIntent(intent: Intent): UrlComponents? {
        val uri = intent.data ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host ?: return null

        val shortioClidUrl = withContext(Dispatchers.IO) {
            HandleClick(uri.toString())
        }
        if (shortioClidUrl != null) {
            savedDestinationUrl =  shortioClidUrl
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
     * HandleClick() is used to track the click
     * Parameters: It takes uriString of type String as parameter.
     * Returns String
     */
    suspend private fun HandleClick(uriString: String): String? = withContext(Dispatchers.IO) {
        isSdkInitialized()
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

    /**
     * It creates a Secure URL
     * parameters: originalURL: String
     * Returns: SecureResult which includes securedOriginalURL and securedShortUrl
     */
    fun createSecure(originalURL: String): SecureResult {
        isSdkInitialized()
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
     * trackConversion() method is used to track the conversion.
     * parameters: originalURL: String, clid: String, conversionId: String? = nil
     * conversionId can be 'signup', 'purchase', 'download', etc.
     * Returns: Result Boolean based on status code
     */
    suspend fun trackConversion(
        originalURL: String? = null,
        clid: String? = null,
        conversionId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        isSdkInitialized()
        try {
            val originalURLString = if (originalURL.isNullOrEmpty()){
                savedDestinationUrl
            } else {
                originalURL
            }
            val originalUri = URI(originalURLString)
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
