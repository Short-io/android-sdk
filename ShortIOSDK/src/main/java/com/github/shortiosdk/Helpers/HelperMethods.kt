package com.github.shortiosdk.Helpers

import android.net.Uri


fun extractClidFromUrl(urlString: String): String? {
    return try {
        val uri = Uri.parse(urlString)
        uri.getQueryParameter("clid")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
