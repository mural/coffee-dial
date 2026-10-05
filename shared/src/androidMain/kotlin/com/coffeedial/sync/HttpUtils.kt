package com.coffeedial.sync

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun httpPostJson(
    url: String,
    jsonBody: String,
    headers: Map<String, String>
): String = withContext(Dispatchers.IO) {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Accept", "application/json")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
        connectTimeout = 15_000
        readTimeout = 15_000
    }
    conn.outputStream.use { os ->
        os.write(jsonBody.encodeToByteArray())
        os.flush()
    }
    val status = conn.responseCode
    val stream = if (status in 200..299) conn.inputStream else conn.errorStream
    val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    if (status !in 200..299) {
        throw IllegalStateException("HTTP Error $status: $responseText")
    }
    responseText
}

actual suspend fun httpGetText(url: String, headers: Map<String, String>): String =
    withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            throw IllegalStateException("HTTP Error $status: $responseText")
        }
        responseText
    }
