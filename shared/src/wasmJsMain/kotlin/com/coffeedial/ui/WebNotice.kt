@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.coffeedial.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

actual fun applyWebShellLanguage(english: Boolean) {
    try {
        if (english) applyEnglishShell() else applySpanishShell()
    } catch (_: Exception) {
    }
}

private fun applyEnglishShell(): Unit = js("globalThis.coffeeApplyShell(true)")

private fun applySpanishShell(): Unit = js("globalThis.coffeeApplyShell(false)")

actual fun updateWebNotice(isLoggedIn: Boolean) {
    try {
        val el = document.getElementById("web-notice") as? HTMLElement ?: return
        val next = if (isLoggedIn) "none" else "block"
        if (el.style.display == next) return
        el.style.display = next
        // The canvas is measured while this notice is still hidden. Defer a
        // resize so the bar is laid out inside the shorter app box.
        resizeOnNextFrame()
    } catch (_: Exception) {
    }
}

private fun resizeOnNextFrame(): Unit =
    js("window.requestAnimationFrame(()=>window.dispatchEvent(new Event('resize')))")
