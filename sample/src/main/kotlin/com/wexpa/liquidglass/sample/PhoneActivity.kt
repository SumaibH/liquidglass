package com.wexpa.liquidglass.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * The Calm navigation comparison scene ([PhoneScene], the one the desktop replay renders) on a
 * device or emulator, for Android verification of the current Calm selector.
 *
 * `adb shell am start -n com.wexpa.liquidglass.sample/.PhoneActivity [--ez natural true]`
 */
class PhoneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val natural = intent?.getBooleanExtra("natural", true) ?: true
        setContent {
            var selected by remember { mutableIntStateOf(0) }
            val palette = if (natural) PhonePalette.Natural else PhonePalette.Recorded
            Box(Modifier.fillMaxSize().background(palette.page), contentAlignment = Alignment.BottomCenter) {
                PhoneScene(selected, { selected = it }, Modifier.navigationBarsPadding().testTag("phone-scene"), palette = palette)
            }
        }
    }
}
