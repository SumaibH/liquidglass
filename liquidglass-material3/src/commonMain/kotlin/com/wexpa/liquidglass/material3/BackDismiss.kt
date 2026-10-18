package com.wexpa.liquidglass.material3

import androidx.compose.runtime.Composable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Calls [onBack] on the platform's back gesture or button while [enabled]: Android's system back,
 * Escape on desktop and the browser's back where the host wires them. Material's dialogs and sheets
 * live in their own windows and get this from the window; the glass ones live in the app's window,
 * so they register it themselves. A host without a navigation-event dispatcher gets nothing, never
 * a crash.
 */
@Composable
internal fun DismissOnBack(enabled: Boolean = true, onBack: () -> Unit) {
    if (LocalNavigationEventDispatcherOwner.current == null) return
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state, isBackEnabled = enabled, onBackCompleted = onBack)
}
