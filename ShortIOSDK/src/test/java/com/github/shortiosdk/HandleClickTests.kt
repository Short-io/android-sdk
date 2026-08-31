package com.github.shortiosdk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** handleClick uses HttpURLConnection, which an OkHttp interceptor cannot observe. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HandleClickTests {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        ShortioSdk.resetForTesting()
    }

    @After
    fun tearDown() {
        server.shutdown()
        ShortioSdk.resetForTesting()
    }

    private fun redirectTo(location: String) =
        MockResponse().setResponseCode(302).setHeader("Location", location)

    @Test
    fun utmMediumIsAppendedExactlyOnce() = runTest {
        repeat(3) { server.enqueue(redirectTo("https://example.com/dest")) }

        ShortioSdk.handleClick(server.url("/abc").toString())
        assertEquals("/abc?utm_medium=android", server.takeRequest().path)

        ShortioSdk.handleClick(server.url("/abc?clid=123").toString())
        assertEquals("/abc?clid=123&utm_medium=android", server.takeRequest().path)

        ShortioSdk.handleClick(server.url("/abc?utm_medium=android").toString())
        assertEquals("/abc?utm_medium=android", server.takeRequest().path)
    }

    @Test
    fun utmMediumIsAppendedToTheQueryEvenWhenTheUrlHasAFragment() = runTest {
        server.enqueue(redirectTo("https://example.com/dest"))

        ShortioSdk.handleClick(server.url("/abc").toString() + "#section")

        assertEquals("/abc?utm_medium=android", server.takeRequest().path)
    }

    @Test
    fun theLocationHeaderIsReturnedAsTheDestination() = runTest {
        server.enqueue(redirectTo("https://example.com/dest"))

        assertEquals(
            "https://example.com/dest",
            ShortioSdk.handleClick(server.url("/abc").toString())
        )
        assertEquals("HEAD", server.takeRequest().method)
    }

    @Test
    fun redirectIsReportedRatherThanFollowed() = runTest {
        // A cross-scheme destination is refused before the redirect flag applies.
        val destination = server.url("/dest").toString()
        server.enqueue(redirectTo(destination))
        server.enqueue(MockResponse().setResponseCode(200))

        assertEquals(destination, ShortioSdk.handleClick(server.url("/abc").toString()))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun aResponseWithoutALocationHeaderYieldsNull() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        assertNull(ShortioSdk.handleClick(server.url("/abc").toString()))
    }

    @Test
    fun anUnreachableServerYieldsNull() = runTest {
        val url = server.url("/abc").toString()
        server.shutdown()

        assertNull(ShortioSdk.handleClick(url))
    }
}
