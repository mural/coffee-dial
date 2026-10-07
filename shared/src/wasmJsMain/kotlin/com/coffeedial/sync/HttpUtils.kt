package com.coffeedial.sync

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual suspend fun httpPostJson(
    url: String,
    jsonBody: String,
    headers: Map<String, String>
): String {
    val reqHeaders = Headers()
    reqHeaders.append("Content-Type", "application/json")
    headers.forEach { (k, v) -> reqHeaders.append(k, v) }

    val init = createPostInit(reqHeaders, jsonBody)
    val response = window.fetch(url, init).await()
    val text = response.text().await().toString()
    if (!response.ok) {
        throw httpFailure(response.status.toInt(), text)
    }
    return text
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual suspend fun httpGetText(url: String, headers: Map<String, String>): String {
    val reqHeaders = Headers()
    headers.forEach { (k, v) -> reqHeaders.append(k, v) }

    val init = createGetInit(reqHeaders)
    val response = window.fetch(url, init).await()
    val text = response.text().await().toString()
    if (!response.ok) {
        throw httpFailure(response.status.toInt(), text)
    }
    return text
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun createPostInit(headers: Headers, body: String): RequestInit =
    js("({ method: 'POST', headers: headers, body: body })")

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun createGetInit(headers: Headers): RequestInit =
    js("({ method: 'GET', headers: headers })")
