package com.github.shortiosdk

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
class CreateSecureTests {

    @Test
    fun securedUrlDecryptsBackAndNeverReusesAnIv() {
        val original = "https://example.com/private?token=1"
        val result = ShortioSdk.createSecure(original)

        assertEquals(true, result.securedOriginalURL.startsWith("shortsecure://"))
        assertEquals(true, result.securedShortUrl.startsWith("#"))

        val payload = result.securedOriginalURL.removePrefix("shortsecure://")
        val cipherText = Base64.decode(payload.substringBefore("?"), Base64.NO_WRAP)
        val iv = Base64.decode(payload.substringAfter("?"), Base64.NO_WRAP)
        val key = Base64.decode(result.securedShortUrl.removePrefix("#"), Base64.NO_WRAP)

        assertEquals(12, iv.size)
        assertEquals(16, key.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        assertEquals(original, String(cipher.doFinal(cipherText), Charsets.UTF_8))

        val ivs = (1..5).map {
            ShortioSdk.createSecure(original)
                .securedOriginalURL.removePrefix("shortsecure://").substringAfter("?")
        }
        assertEquals(5, ivs.toSet().size)
    }
}
