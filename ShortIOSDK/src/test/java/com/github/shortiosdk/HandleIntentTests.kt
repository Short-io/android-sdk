package com.github.shortiosdk

import android.content.Intent
import android.net.Uri
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class HandleIntentTests {

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

    private fun viewIntent(url: String) = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    @Test
    fun aNonWebSchemeIsIgnored() = runTest {
        assertNull(ShortioSdk.handleIntent(viewIntent("shortsecure://payload")))
    }

    @Test
    fun anUnresolvableLinkLeavesTheDestinationNull() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val components = ShortioSdk.handleIntent(viewIntent(server.url("/abc").toString()))!!

        assertNull(components.destinationUrl)
        assertEquals("", ShortioSdk.clid)
    }

    @Test
    fun webLinksYieldComponentsWithUtmStrippedAndTheClidCaptured() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(302).setHeader(
                "Location", "https://example.com/dest?utm_medium=android&clid=c1&id=7"
            )
        )

        val link = server.url("/abc?clid=c1").toString()
        val components = ShortioSdk.handleIntent(viewIntent(link))!!

        assertEquals("http", components.scheme)
        assertEquals("abc", components.path)
        assertEquals("https://example.com/dest?clid=c1&id=7", components.destinationUrl)
        assertEquals("c1", ShortioSdk.clid)
    }
}
