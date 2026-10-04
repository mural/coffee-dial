package com.coffeedial.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun httpPostJson(url: String, jsonBody: String, headers: Map<String, String>): String =
    withContext(Dispatchers.Default) {
        "{}"
    }

actual suspend fun httpGetText(url: String, headers: Map<String, String>): String =
    withContext(Dispatchers.Default) {
        "{}"
    }
