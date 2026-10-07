package com.coffeedial.sync

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun httpPostJson(
    url: String,
    jsonBody: String,
    headers: Map<String, String>
): String = withContext(Dispatchers.Default) {
    suspendCancellableCoroutine { continuation ->
        val nsUrl = NSURL.URLWithString(url) ?: run {
            continuation.resumeWithException(IllegalArgumentException("Invalid URL: $url"))
            return@suspendCancellableCoroutine
        }
        val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
            setHTTPMethod("POST")
            setValue("application/json", forHTTPHeaderField = "Content-Type")
            setValue("application/json", forHTTPHeaderField = "Accept")
            headers.forEach { (k, v) -> setValue(v, forHTTPHeaderField = k) }
            val bytes = jsonBody.encodeToByteArray()
            val nsData = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            setHTTPBody(nsData)
        }

        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) {
                data,
                response,
                error
            ->
            dispatch_async(dispatch_get_main_queue()) {
                if (continuation.isActive) {
                    if (error != null) {
                        continuation.resumeWithException(
                            IllegalStateException(error.localizedDescription)
                        )
                    } else {
                        val httpResponse = response as? NSHTTPURLResponse
                        val statusCode = httpResponse?.statusCode?.toInt() ?: 500
                        val text = data?.let {
                            NSString.create(
                                data = it,
                                encoding = NSUTF8StringEncoding
                            )?.toString()
                        }.orEmpty()

                        if (statusCode !in 200..299) {
                            continuation.resumeWithException(
                                httpFailure(statusCode.toInt(), text)
                            )
                        } else {
                            continuation.resume(text)
                        }
                    }
                }
            }
        }
        task.resume()
        continuation.invokeOnCancellation { task.cancel() }
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual suspend fun httpGetText(url: String, headers: Map<String, String>): String =
    withContext(Dispatchers.Default) {
        suspendCancellableCoroutine { continuation ->
            val nsUrl = NSURL.URLWithString(url) ?: run {
                continuation.resumeWithException(IllegalArgumentException("Invalid URL: $url"))
                return@suspendCancellableCoroutine
            }
            val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
                setHTTPMethod("GET")
                setValue("application/json", forHTTPHeaderField = "Accept")
                headers.forEach { (k, v) -> setValue(v, forHTTPHeaderField = k) }
            }

            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) {
                    data,
                    response,
                    error
                ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resumeWithException(
                                IllegalStateException(error.localizedDescription)
                            )
                        } else {
                            val httpResponse = response as? NSHTTPURLResponse
                            val statusCode = httpResponse?.statusCode?.toInt() ?: 500
                            val text = data?.let {
                                NSString.create(
                                    data = it,
                                    encoding = NSUTF8StringEncoding
                                )?.toString()
                            }.orEmpty()

                            if (statusCode !in 200..299) {
                                continuation.resumeWithException(
                                    httpFailure(statusCode.toInt(), text)
                                )
                            } else {
                                continuation.resume(text)
                            }
                        }
                    }
                }
            }
            task.resume()
            continuation.invokeOnCancellation { task.cancel() }
        }
    }
