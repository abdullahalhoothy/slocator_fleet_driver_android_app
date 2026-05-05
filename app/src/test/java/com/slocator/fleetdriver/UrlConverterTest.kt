package com.slocator.fleetdriver

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLDecoder

class UrlConverterTest {
    
    // Simulate Android Uri parsing locally without Robolectric
    private fun getQueryParameter(url: String, key: String): String? {
        val query = url.substringAfter("?", "")
        if (query.isEmpty()) return null
        val pairs = query.split("&")
        for (pair in pairs) {
            val parts = pair.split("=")
            if (parts.size == 2 && parts[0] == key) {
                return URLDecoder.decode(parts[1], "UTF-8")
            }
        }
        return null
    }

    private fun convertApiUrlToWebUrl(apiUrl: String): String {
        try {
            if (getQueryParameter(apiUrl, "api") == "1") {
                val origin = getQueryParameter(apiUrl, "origin") ?: ""
                val dest = getQueryParameter(apiUrl, "destination") ?: ""
                val wpsStr = getQueryParameter(apiUrl, "waypoints") ?: ""
                
                val wps = wpsStr.split("|").filter { it.isNotBlank() }
                
                val builder = java.lang.StringBuilder("https://www.google.com/maps/dir/")
                
                // Empty first segment tells Maps to use Current Location as the starting point
                builder.append("/")
                
                // The API's origin becomes the first destination to visit
                if (origin.isNotBlank()) {
                    builder.append(origin).append("/")
                }
                
                for (wp in wps) {
                    builder.append(wp).append("/")
                }
                
                if (dest.isNotBlank()) {
                    builder.append(dest).append("/")
                }
                
                return builder.toString()
            }
        } catch (e: Exception) {
            // ignore
        }
        return apiUrl
    }

    @Test
    fun testConversion() {
        val inputUrl = "https://www.google.com/maps/dir/?api=1&origin=21.45,39.39&destination=21.46,39.40&waypoints=21.61,39.14%7C21.62,39.15"
        // Expected url omits the origin, leaving the first segment blank to start from current location.
        val expected = "https://www.google.com/maps/dir//21.45,39.39/21.61,39.14/21.62,39.15/21.46,39.40/"
        assertEquals(expected, convertApiUrlToWebUrl(inputUrl))
        
        val inputUrl2 = "https://www.google.com/maps/dir/?api=1&origin=11.11,22.22&destination=33.33,44.44"
        val expected2 = "https://www.google.com/maps/dir//11.11,22.22/33.33,44.44/"
        assertEquals(expected2, convertApiUrlToWebUrl(inputUrl2))
    }
}