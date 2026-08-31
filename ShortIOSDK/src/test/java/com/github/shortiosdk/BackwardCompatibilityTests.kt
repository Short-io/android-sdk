package com.github.shortiosdk

import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class BackwardCompatibilityTests {

    @Test
    fun theOldShortenUrlNameStillResolves() {
        assertEquals(baseURL, shortenUrl)
    }

    @Test
    fun urlComponentsStillConstructsWithoutADestination() {
        val c = UrlComponents(
            scheme = "https",
            host = "x.gy",
            path = "abc",
            query = null,
            fragment = null,
            fullUrl = "https://x.gy/abc"
        )
        assertEquals(null, c.destinationUrl)
    }
}
