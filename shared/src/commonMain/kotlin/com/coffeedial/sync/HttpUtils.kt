package com.coffeedial.sync

expect suspend fun httpPostJson(url: String, jsonBody: String, headers: Map<String, String> = emptyMap()): String
expect suspend fun httpGetText(url: String, headers: Map<String, String> = emptyMap()): String
