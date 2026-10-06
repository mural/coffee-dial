package com.coffeedial.ui

import kotlinx.browser.document
import org.w3c.dom.HTMLElement

actual fun updateWebNotice(isLoggedIn: Boolean) {
    try {
        val el = document.getElementById("web-notice") as? HTMLElement
        if (el != null) {
            el.style.display = if (isLoggedIn) "none" else "block"
        }
    } catch (_: Exception) {
    }
}
