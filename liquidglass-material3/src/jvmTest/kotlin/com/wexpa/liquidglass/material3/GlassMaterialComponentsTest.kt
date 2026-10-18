package com.wexpa.liquidglass.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * The Material 3 glass components in one app (Phase 3, 18 October): rendered for the record, and
 * driven: taps select, clicks fire, toggles toggle, overlays open and dismiss.
 */
@OptIn(ExperimentalTestApi::class)
class GlassMaterialComponentsTest {
    private val out = File("build/reports/atlas/material3").apply { mkdirs() }

    /** Colourful rows to refract. */
    @Composable private fun Page() = LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 72.dp, bottom = 120.dp)) {
        items(30) { i ->
            val hue = listOf(Color(0xFFE35D6A), Color(0xFF4C8DF6), Color(0xFF3CC47C), Color(0xFFF2B33D), Color(0xFF9B6CF6))[i % 5]
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(hue, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(12.dp))
                Column { Text("Row $i", color = MaterialTheme.colorScheme.onBackground); Text("Glass over a list", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .6f)) }
            }
        }
    }

    /** Album art to refract: bright gradients under the glass, as in a music library. */
    @Composable private fun Albums() = LazyVerticalGrid(
        GridCells.Fixed(2), Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 104.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val covers = listOf(
            Color(0xFFFF5E62) to Color(0xFFFF9966), Color(0xFF4776E6) to Color(0xFF8E54E9),
            Color(0xFF11998E) to Color(0xFF38EF7D), Color(0xFFF7971E) to Color(0xFFFFD200),
            Color(0xFFEC008C) to Color(0xFFFC6767), Color(0xFF00C6FF) to Color(0xFF0072FF),
        )
        val titles = listOf("Midnight Drive", "Glasshouse", "Evergreen", "Golden Hour", "Neon Bloom", "Open Water")
        items(12) { i ->
            val (from, to) = covers[i % covers.size]
            Column {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(from, to))))
                Text(titles[i % titles.size], Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    /** One screen built only from the glass components, over [Albums]. */
    @Composable private fun Showcase(dark: Boolean, onTab: (Int) -> Unit = {}, tab: Int = 0) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            var checked by remember { mutableStateOf(true) }
            var value by remember { mutableFloatStateOf(.62f) }
            var segment by remember { mutableIntStateOf(0) }
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Library") },
                        navigationIcon = { IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                        actions = { IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, "More") } },
                    )
                },
                bottomBar = {
                    NavigationBar(accessory = { Icon(Icons.Default.Search, "Search") }) {
                        listOf(Icons.Default.Home to "Home", Icons.Default.Favorite to "Favourites", Icons.Default.Person to "Profile")
                            .forEachIndexed { i, (icon, label) ->
                                NavigationBarItem(selected = tab == i, onClick = { onTab(i) }, icon = { Icon(icon, null) },
                                    label = { Text(label) }, modifier = Modifier.testTag("tab$i"))
                            }
                    }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize()) {
                    Albums()
                    Column(
                        Modifier.padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("Albums", "Songs", "Artists").forEachIndexed { i, l ->
                                SegmentedButton(selected = segment == i, onClick = { segment = i },
                                    shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = {}) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play") }
                            OutlinedButton(onClick = {}) { Text("Shuffle") }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {}) { Icon(Icons.Default.Add, "Add") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = true, onClick = {}, label = { Text("Downloaded") },
                                leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) })
                            AssistChip(onClick = {}, label = { Text("Recently added") })
                        }
                        Spacer(Modifier.height(298.dp))  // the card sits over one row of covers
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Crossfade", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Switch(checked = checked, onCheckedChange = { checked = it })
                                }
                                Text("Volume", style = MaterialTheme.typography.bodyMedium)
                                Slider(value = value, onValueChange = { value = it }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    // The window is the phone: 390 x 844 dp. The default test window (1024 x 768 px) would squeeze it.
    private fun shoot(name: String, dark: Boolean) = runDesktopComposeUiTest(780, 1688, testTimeout = 4.minutes) {
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(Modifier.size(390.dp, 844.dp).testTag("app")) { Showcase(dark) }
            }
        }
        waitForIdle()
        val img = onNodeWithTag("app").captureToImage().toAwtImage()
        ImageIO.write(img, "png", File(out, "$name.png"))
    }

    @Test fun showcaseDark() = shoot("showcase-dark", dark = true)
    @Test fun showcaseLight() = shoot("showcase-light", dark = false)

    @Test fun navigationTabsSelectThroughTheGlassBar() = runDesktopComposeUiTest(390, 844, testTimeout = 4.minutes) {
        var tab by mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(390.dp, 844.dp)) { Showcase(true, { tab = it }, tab) }
            }
        }
        waitForIdle()
        // Two glass tab bars are on screen: the segmented control and the navigation bar, the lowest.
        val bars = onAllNodesWithTag(com.wexpa.liquidglass.GlassTabBarTestTag, useUnmergedTree = true)
        val lowest = bars.fetchSemanticsNodes().withIndex().maxBy { it.value.boundsInRoot.top }.index
        val bar = bars[lowest]
        bar.performTouchInput { val w = width / 3f; down(Offset(w * 2.5f, height / 2f)); advanceEventTime(60); up() }
        // Wait for the selection, not for idle: on hosted runners the bar's settling lens kept the
        // test from ever idling (CI runs 39147674357 and the next, 15 min each).
        waitUntil(timeoutMillis = 120_000) { tab == 2 }
        assertEquals(2, tab, "a tap on the third tab did not select it")
    }

    @Test fun togglesToggleAndALongPressShowsATooltip() = runDesktopComposeUiTest(390, 700, testTimeout = 4.minutes) {
        var liked by mutableStateOf(false)
        var clicks by mutableIntStateOf(0)
        lateinit var tip: androidx.compose.material3.TooltipState
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    Scaffold(Modifier.size(390.dp, 700.dp)) { _ ->
                        Box(Modifier.fillMaxSize()) {
                            Page()
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(80.dp)) {
                                IconToggleButton(checked = liked, onCheckedChange = { liked = it }, modifier = Modifier.testTag("toggle")) {
                                    Icon(Icons.Default.Favorite, "Like")
                                }
                                tip = androidx.compose.material3.rememberTooltipState(isPersistent = true)
                                TooltipBox(androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                    { PlainTooltip { Text("Add", Modifier.testTag("tip")) } }, tip) {
                                    IconButton(onClick = { clicks++ }, modifier = Modifier.testTag("anchor")) { Icon(Icons.Default.Add, "Add") }
                                }
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("toggle").performClick(); waitForIdle()
        assertTrue(liked, "the glass toggle did not toggle on")
        onNodeWithTag("toggle").performClick(); waitForIdle()
        assertTrue(!liked, "the glass toggle did not toggle off")
        // A tap still reaches the anchor: the tooltip observes input and never consumes it.
        onNodeWithTag("anchor").performClick(); waitForIdle()
        assertEquals(1, clicks, "the tooltip's anchor lost its click")
        onNodeWithTag("anchor").performTouchInput { longClick() }
        waitUntil(timeoutMillis = 10_000) { tip.isVisible }
        onNodeWithTag("tip", useUnmergedTree = true).assertExists()
    }

    @Test fun backDismissesOverlaysAndATallSheetStopsHalfway() = runDesktopComposeUiTest(390, 700, testTimeout = 4.minutes) {
        val dispatcher = androidx.navigationevent.NavigationEventDispatcher()
        val back = androidx.navigationevent.DirectNavigationEventInput()
        dispatcher.addInput(back)
        val owner = object : androidx.navigationevent.NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = dispatcher
        }
        var dialog by mutableStateOf(false)
        var menu by mutableStateOf(false)
        var sheet by mutableStateOf(false)
        lateinit var sheetState: SheetState
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f),
                androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner provides owner) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    Scaffold(Modifier.size(390.dp, 700.dp)) { _ ->
                        Box(Modifier.fillMaxSize()) {
                            Page()
                            Box {
                                Text("anchor")
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(text = { Text("First") }, onClick = { menu = false })
                                }
                            }
                            if (dialog) AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {
                                TextButton(onClick = { dialog = false }) { Text("OK") }
                            }, title = { Text("Title") })
                            sheetState = rememberModalBottomSheetState()
                            if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }, sheetState = sheetState) {
                                // Taller than half the 700 px window, so the sheet has a partial stop.
                                Box(Modifier.fillMaxWidth().height(560.dp).testTag("tall"))
                            }
                        }
                    }
                }
            }
        }
        waitForIdle()
        dialog = true; waitForIdle()
        back.backCompleted(); waitForIdle()
        assertTrue(!dialog, "back did not dismiss the dialog")
        menu = true; waitForIdle()
        back.backCompleted(); waitForIdle()
        assertTrue(!menu, "back did not dismiss the menu")

        sheet = true
        waitUntil(timeoutMillis = 20_000) { sheetState.targetValue != androidx.compose.material3.SheetValue.Hidden && !sheetState.progress.isRunning }
        assertEquals(androidx.compose.material3.SheetValue.PartiallyExpanded, sheetState.currentValue,
            "a sheet taller than half the window must open at its partial stop")
        val top = onNodeWithTag("tall", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue(top in 300f..400f, "the partial stop shows half the window; the sheet's top is at $top")
        back.backCompleted()
        waitUntil(timeoutMillis = 20_000) { !sheet }
    }

    @Test fun buttonsClickAndOverlaysOpenAndDismiss() = runDesktopComposeUiTest(390, 700, testTimeout = 4.minutes) {
        var clicks by mutableIntStateOf(0)
        var dialog by mutableStateOf(false)
        var sheet by mutableStateOf(false)
        var menu by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    Scaffold(Modifier.size(390.dp, 700.dp)) { _ ->
                        Box(Modifier.fillMaxSize()) {
                            Page()
                            Column(Modifier.padding(24.dp)) {
                                Button(onClick = { clicks++ }, modifier = Modifier.testTag("button")) { Text("Tap") }
                                Box {
                                    TextButton(onClick = { menu = true }, modifier = Modifier.testTag("menu-anchor")) { Text("Menu") }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text("First") }, onClick = { menu = false }, modifier = Modifier.testTag("menu-item"))
                                    }
                                }
                            }
                            if (dialog) AlertDialog(onDismissRequest = { dialog = false }, confirmButton = {
                                TextButton(onClick = { dialog = false }, modifier = Modifier.testTag("confirm")) { Text("OK") }
                            }, title = { Text("Title") }, text = { Text("Body") })
                            if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) { Text("Sheet", Modifier.padding(24.dp).testTag("sheet-text")) }
                        }
                    }
                }
            }
        }
        waitForIdle()
        onNodeWithTag("button").performClick(); waitForIdle()
        assertEquals(1, clicks, "the glass button did not click")

        dialog = true; waitForIdle()
        val dialogShot = onRoot().captureToImage().toAwtImage()
        ImageIO.write(dialogShot, "png", File(out, "dialog.png"))
        onNodeWithTag("confirm", useUnmergedTree = true).assertExists().performClick(); waitForIdle()
        assertTrue(!dialog, "the dialog's button did not dismiss it")

        sheet = true; waitForIdle()
        onNodeWithTag("sheet-text", useUnmergedTree = true).assertExists()
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "sheet.png"))
        sheet = false; waitForIdle()

        onNodeWithTag("menu-anchor").performClick(); waitForIdle()
        assertTrue(menu)
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(out, "menu.png"))
        onNodeWithTag("menu-item", useUnmergedTree = true).assertExists().performClick(); waitForIdle()
        assertTrue(!menu, "the menu item did not close the menu")
    }
}
