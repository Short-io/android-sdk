package com.github.shortiosdk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ShortIOSdkTests {

    private lateinit var stub: StubInterceptor

    @Before
    fun setUp() {
        stub = StubInterceptor()
        ShortioSdk.resetForTesting(stub.client())
    }

    @After
    fun tearDown() {
        ShortioSdk.resetForTesting()
    }

    private fun initialized() = ShortioSdk.initialize("sk_test", "example.short.gy")

    @Test
    fun configurationIsStoredOnceAndGuardsUninitializedUse() {
        assertThrows(IllegalStateException::class.java) { ShortioSdk.isSdkInitialized() }

        initialized()
        assertEquals("sk_test", ShortioSdk.apiKey)
        assertEquals("example.short.gy", ShortioSdk.domain)
        assertTrue(ShortioSdk.isSdkInitialized())

        ShortioSdk.initialize("sk_second", "second.short.gy")
        assertEquals("sk_test", ShortioSdk.apiKey)
        assertEquals("example.short.gy", ShortioSdk.domain)
    }

    @Test
    fun initializeNeverWritesTheApiKeyToStdout() {
        val captured = java.io.ByteArrayOutputStream()
        val original = System.out
        System.setOut(java.io.PrintStream(captured))
        try {
            ShortioSdk.initialize("sk_super_secret", "example.short.gy")
        } finally {
            System.setOut(original)
        }

        assertFalse(captured.toString().contains("sk_super_secret"))
    }

    @Test
    fun createShortLinkPostsAuthenticatedJsonAndMapsSuccess() {
        initialized()
        stub.respond(200, """{"originalURL":"https://example.com","shortURL":"https://x.gy/abc"}""")

        val result = ShortioSdk.createShortLink(
            ShortIOParameters(
                originalURL = "https://example.com",
                expiresAt = StringOrInt.IntVal(1893456000)
            )
        )

        val request = stub.lastRequest
        assertEquals("POST", request.method)
        assertEquals("https://api.short.io/links/public", request.url.toString())
        assertEquals("sk_test", request.header("authorization"))
        assertTrue(stub.lastBody.contains("\"expiresAt\":1893456000"))

        assertTrue(result is ShortIOResult.Success)
        assertEquals("https://x.gy/abc", (result as ShortIOResult.Success).data.shortURL)
    }

    @Test
    fun createShortLinkForwardsAServerErrorWithItsStatusCode() {
        initialized()
        stub.respond(400, """{"success":false,"code":"DomainNotFound","message":"Domain not found"}""")

        val result = ShortioSdk.createShortLink(ShortIOParameters("https://example.com"))

        assertTrue(result is ShortIOResult.Error)
        assertEquals("DomainNotFound", (result as ShortIOResult.Error).data.code)
        assertEquals("Domain not found", result.data.message)
        assertEquals(400, result.data.statusCode)
    }

    @Test
    fun createShortLinkReportsAnEmptySuccessBodyAsMalformed() {
        initialized()
        stub.respond(200, "")

        val result = ShortioSdk.createShortLink(ShortIOParameters("https://example.com"))

        assertTrue(result is ShortIOResult.Error)
        assertEquals("MALFORMED_SUCCESS", (result as ShortIOResult.Error).data.code)
        assertEquals(200, result.data.statusCode)
    }

    @Test
    fun createShortLinkReportsANonJsonErrorBodyAsInvalidJson() {
        initialized()
        stub.respond(502, "<html>Bad Gateway</html>")

        val result = ShortioSdk.createShortLink(ShortIOParameters("https://example.com"))

        assertTrue(result is ShortIOResult.Error)
        assertEquals("INVALID_JSON", (result as ShortIOResult.Error).data.code)
        assertEquals(502, result.data.statusCode)
    }

    @Test
    fun createShortLinkUsesTheConfiguredDomainOnlyWhenNoneIsGiven() {
        initialized()
        stub.respond(200, """{"originalURL":"https://example.com"}""")

        val fallback = ShortIOParameters(originalURL = "https://example.com")
        ShortioSdk.createShortLink(fallback)
        assertEquals("example.short.gy", fallback.domain)

        val explicit = ShortIOParameters("https://example.com", domain = "other.short.gy")
        ShortioSdk.createShortLink(explicit)
        assertEquals("other.short.gy", explicit.domain)
    }

    @Test
    fun trackConversionReportsFailureRatherThanThrowingWithNoDomain() = runTest {
        assertEquals(false, ShortioSdk.trackConversion(conversionId = "purchase"))
        assertEquals(0, stub.requests.size)
    }

    @Suppress("DEPRECATION")
    @Test
    fun theDeprecatedOverloadFallsBackToTheConfiguredKeyRatherThanSendingNull() {
        initialized()
        stub.respond(200, """{"originalURL":"https://example.com"}""")

        ShortioSdk.createShortLink(ShortIOParameters("https://example.com"), null)

        assertEquals("sk_test", stub.lastRequest.header("authorization"))
    }

    @Test
    fun outOfRangeParametersAreRejectedAtConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            ShortIOParameters("https://example.com", clicksLimit = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ShortIOParameters("https://example.com", splitPercent = 101)
        }
        assertEquals(1, ShortIOParameters("https://example.com", splitPercent = 1).splitPercent)
    }

    @Test
    fun trackConversionFallsBackToTheConfiguredDomainAndStoredClid() = runTest {
        initialized()
        ShortioSdk.clid = "clid_from_open"
        stub.respond(200)

        ShortioSdk.trackConversion(conversionId = "purchase")

        assertEquals(
            "https://example.short.gy/.shortio/conversion?c=purchase&clid=clid_from_open",
            stub.lastRequest.url.toString()
        )
    }

    @Test
    fun trackConversionPrefersExplicitValuesAndPercentEncodesThem() = runTest {
        initialized()
        ShortioSdk.clid = "clid_from_open"
        stub.respond(200)

        ShortioSdk.trackConversion(
            clid = "a b&c",
            domain = "other.short.gy",
            conversionId = "sign up"
        )

        assertEquals("other.short.gy", stub.lastRequest.url.host)
        assertEquals("c=sign+up&clid=a+b%26c", stub.lastRequest.url.encodedQuery)
    }

    @Test
    fun trackConversionReportsTheTransportStatus() = runTest {
        initialized()

        stub.respond(200)
        assertTrue(ShortioSdk.trackConversion(conversionId = "purchase"))

        stub.respond(404)
        assertEquals(false, ShortioSdk.trackConversion(conversionId = "purchase"))
    }
}
