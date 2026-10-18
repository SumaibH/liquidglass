package com.wexpa.liquidglass.sample.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import com.wexpa.liquidglass.sample.LoupeScreen
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * The live demo: Atlas Studio (the Loupe screen) or, at `#material3`, a Material 3 app built from
 * `liquidglass-material3`. Each links to the other; the address keeps the choice.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        var material by remember { mutableStateOf(window.location.hash == "#material3") }
        fun show(m: Boolean) { material = m; window.location.hash = if (m) "material3" else "" }
        if (material) MaterialShowcase(onSwitchScreen = { show(false) })
        else Box {
            LoupeScreen()
            Text(
                "Material 3 demo →", color = Color.White, fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .background(Color(0x99000000), RoundedCornerShape(50)).clickable { show(true) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
