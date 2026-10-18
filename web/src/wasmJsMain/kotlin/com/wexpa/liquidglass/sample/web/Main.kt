package com.wexpa.liquidglass.sample.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.wexpa.liquidglass.sample.LoupeScreen
import kotlinx.browser.document

/** Atlas Studio on the web: the Loupe screen filling the page. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        LoupeScreen()
    }
}
