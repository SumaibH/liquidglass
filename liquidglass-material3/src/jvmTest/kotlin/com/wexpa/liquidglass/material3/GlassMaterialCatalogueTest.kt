package com.wexpa.liquidglass.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Every Material 3 glass component, laid out as a catalogue over diagonal colour bands that show the
 * refraction at each edge (Phase 5, 18 October). Rendered for the docs; the interaction checks are in
 * [GlassMaterialComponentsTest].
 */
@OptIn(ExperimentalTestApi::class)
class GlassMaterialCatalogueTest {
    private val out = File("build/reports/atlas/material3").apply { mkdirs() }

    /** Diagonal bands: every glass edge crosses a colour boundary. */
    @Composable private fun Bands(dark: Boolean) = Canvas(Modifier.fillMaxSize()) {
        drawRect(if (dark) Color(0xFF101014) else Color(0xFFF4F2F8))
        val colours = listOf(Color(0xFFFF5E62), Color(0xFFFFB347), Color(0xFF38EF7D), Color(0xFF00C6FF), Color(0xFF8E54E9))
        val band = 46.dp.toPx()
        var x = -size.height
        var i = 0
        while (x < size.width) {
            val p = Path().apply {
                moveTo(x, size.height); lineTo(x + band, size.height)
                lineTo(x + band + size.height, 0f); lineTo(x + size.height, 0f); close()
            }
            if (i % 2 == 0) drawPath(p, colours[(i / 2) % colours.size].copy(alpha = if (dark) .85f else .75f))
            x += band; i++
        }
    }

    @Composable private fun Label(text: String) = Text(
        text.uppercase(), color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp, modifier = Modifier.padding(top = 6.dp),
    )

    @Composable private fun Page(dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            Scaffold { _ ->
                Box(Modifier.fillMaxSize()) {
                    Bands(dark)
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
                }
            }
        }
    }

    @Composable private fun Controls(dark: Boolean) = Page(dark) {
        Label("Buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("Button") }
            FilledTonalButton(onClick = {}) { Text("Tonal") }
            ElevatedButton(onClick = {}) { Text("Elevated") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {}) { Text("Outlined") }
            TextButton(onClick = {}) { Text("Text") }
        }
        Label("Icon buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = {}) { Icon(Icons.Default.Favorite, null) }
            FilledIconButton(onClick = {}) { Icon(Icons.Default.Add, null) }
            FilledTonalIconButton(onClick = {}) { Icon(Icons.Default.Share, null) }
            OutlinedIconButton(onClick = {}) { Icon(Icons.Default.Settings, null) }
        }
        Label("Floating action buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallFloatingActionButton(onClick = {}) { Icon(Icons.Default.Edit, null) }
            FloatingActionButton(onClick = {}) { Icon(Icons.Default.Add, null) }
            LargeFloatingActionButton(onClick = {}) { Icon(Icons.Default.Create, null) }
        }
        ExtendedFloatingActionButton(text = { Text("Compose") }, icon = { Icon(Icons.Default.Edit, null) }, onClick = {})
        Label("Chips")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("Assist") })
            FilterChip(selected = true, onClick = {}, label = { Text("Filter") })
            InputChip(selected = false, onClick = {}, label = { Text("Input") })
            SuggestionChip(onClick = {}, label = { Text("Suggest") })
        }
        Label("Selection")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            var on by remember { mutableStateOf(true) }
            var off by remember { mutableStateOf(false) }
            Switch(checked = on, onCheckedChange = { on = it })
            Switch(checked = off, onCheckedChange = { off = it })
            var v by remember { mutableFloatStateOf(.35f) }
            Slider(value = v, onValueChange = { v = it }, modifier = Modifier.weight(1f))
        }
        Label("Cards")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.weight(1f).height(72.dp)) { Text("Card", Modifier.padding(14.dp)) }
            ElevatedCard(Modifier.weight(1f).height(72.dp)) { Text("Elevated", Modifier.padding(14.dp)) }
            OutlinedCard(Modifier.weight(1f).height(72.dp)) { Text("Outlined", Modifier.padding(14.dp)) }
        }
    }

    @Composable private fun Navigation(dark: Boolean) = Page(dark) {
        Label("Top app bars")
        TopAppBar(title = { Text("Title") }, navigationIcon = { IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = {}) { Icon(Icons.Default.Search, null) }; IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) } })
        CenterAlignedTopAppBar(title = { Text("Centred") }, actions = { IconButton(onClick = {}) { Icon(Icons.Default.Person, null) } })
        Label("Tab row and segmented buttons")
        var tab by remember { mutableIntStateOf(1) }
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Day", "Week", "Month", "Year").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        var picked by remember { mutableStateOf(setOf(0, 2)) }
        MultiChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("Bold", "Italic", "Underline").forEachIndexed { i, t ->
                SegmentedButton(checked = i in picked, onCheckedChange = { picked = if (it) picked + i else picked - i },
                    shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(t) }
            }
        }
        Label("Search")
        SearchBar(query = "glass", onQueryChange = {}, onSearch = {}, active = true, onActiveChange = {},
            leadingIcon = { Icon(Icons.Default.Search, null) }) {
            listOf("glass tab bar", "glass buttons").forEach { Text(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        }
        Label("Snackbar")
        Snackbar(action = { TextButton(onClick = {}) { Text("Undo") } }) { Text("Message archived") }
        Label("Bottom app bar")
        BottomAppBar(actions = {
            IconButton(onClick = {}) { Icon(Icons.Default.Check, null) }
            IconButton(onClick = {}) { Icon(Icons.Default.Edit, null) }
            IconButton(onClick = {}) { Icon(Icons.Default.Delete, null) }
        }, floatingActionButton = { FloatingActionButton(onClick = {}) { Icon(Icons.Default.Add, null) } })
    }

    @Composable private fun RailAndMore(dark: Boolean) = Page(dark) {
        Label("Scrollable tab row")
        var tab by remember { mutableIntStateOf(2) }
        ScrollableTabRow(selectedTabIndex = tab) {
            listOf("Featured", "Popular", "New", "Classics", "Live", "Mixes").forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        Label("Navigation rail")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            var item by remember { mutableIntStateOf(0) }
            NavigationRail(Modifier.height(300.dp)) {
                listOf(Icons.Default.Home to "Home", Icons.Default.Favorite to "Saved", Icons.Default.Settings to "Settings")
                    .forEachIndexed { i, (icon, label) ->
                        NavigationRailItem(selected = item == i, onClick = { item = i }, icon = { Icon(icon, null) }, label = { Text(label) })
                    }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Label("Snackbar, action on a new line")
                Snackbar(actionOnNewLine = true, action = { TextButton(onClick = {}) { Text("View") } },
                    dismissAction = { IconButton(onClick = {}) { Icon(Icons.Default.Close, "Dismiss") } }) {
                    Text("Three tracks were added to your library")
                }
            }
        }
    }

    @Composable private fun More(dark: Boolean) = Page(dark) {
        Label("Icon toggle buttons")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconToggleButton(checked = false, onCheckedChange = {}) { Icon(Icons.Default.FavoriteBorder, null) }
            IconToggleButton(checked = true, onCheckedChange = {}) { Icon(Icons.Default.Favorite, null) }
            FilledIconToggleButton(checked = true, onCheckedChange = {}) { Icon(Icons.Default.Star, null) }
            FilledTonalIconToggleButton(checked = true, onCheckedChange = {}) { Icon(Icons.Default.Notifications, null) }
            OutlinedIconToggleButton(checked = true, onCheckedChange = {}) { Icon(Icons.Default.Lock, null) }
        }
        Label("Elevated chips")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedAssistChip(onClick = {}, label = { Text("Assist") })
            ElevatedFilterChip(selected = true, onClick = {}, label = { Text("Filter") })
            ElevatedSuggestionChip(onClick = {}, label = { Text("Suggest") })
        }
        Label("Range slider")
        var range by remember { mutableStateOf(0.2f..0.7f) }
        RangeSlider(value = range, onValueChange = { range = it })
        Label("Leading icon tabs and a scrollable tab row")
        var tab by remember { mutableIntStateOf(0) }
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf(Icons.Default.Home to "Home", Icons.Default.Search to "Browse").forEachIndexed { i, (icon, t) ->
                LeadingIconTab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }, icon = { Icon(icon, null) })
            }
        }
        var scroll by remember { mutableIntStateOf(1) }
        PrimaryScrollableTabRow(selectedTabIndex = scroll) {
            listOf("Featured", "Popular", "New", "Classics", "Live").forEachIndexed { i, t ->
                Tab(selected = scroll == i, onClick = { scroll = i }, text = { Text(t) })
            }
        }
        Label("Surface and a plain tooltip")
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
            Surface(Modifier.size(170.dp, 64.dp), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                Text("A glass Surface", Modifier.padding(16.dp))
            }
            Column {
                Spacer(Modifier.height(40.dp))
                TooltipBox(TooltipDefaults.rememberPlainTooltipPositionProvider(), { PlainTooltip { Text("Add to library") } },
                    rememberTooltipState(initialIsVisible = true, isPersistent = true)) {
                    IconButton(onClick = {}) { Icon(Icons.Default.Add, "Add") }
                }
            }
        }
        Label("A rich tooltip")
        Spacer(Modifier.height(120.dp))
        TooltipBox(TooltipDefaults.rememberRichTooltipPositionProvider(), {
            RichTooltip(title = { Text("Offline") }, action = { TextButton(onClick = {}) { Text("Learn more") } }) {
                Text("Downloaded music plays without a connection.")
            }
        }, rememberTooltipState(initialIsVisible = true, isPersistent = true)) {
            IconButton(onClick = {}) { Icon(Icons.Default.Info, "Info") }
        }
    }

    @Composable private fun Drawer(dark: Boolean) = Page(dark) {
        val state = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Open)
        var item by remember { mutableIntStateOf(0) }
        androidx.compose.material3.ModalNavigationDrawer(drawerState = state, scrimColor = Color.Transparent, drawerContent = {
            ModalDrawerSheet {
                Text("Library", Modifier.padding(horizontal = 24.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium)
                listOf(Icons.Default.Home to "Listen now", Icons.Default.Favorite to "Loved", Icons.Default.Person to "Artists",
                    Icons.Default.Settings to "Settings").forEachIndexed { i, (icon, t) ->
                    NavigationDrawerItem(label = { Text(t) }, selected = item == i, onClick = { item = i }, icon = { Icon(icon, null) })
                }
            }
        }) { Box(Modifier.fillMaxSize()) }
    }

    @Composable private fun Picker(dark: Boolean) = Page(dark) {
        val state = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = 1_792_368_000_000L)
        DatePickerDialog(onDismissRequest = {}, confirmButton = { TextButton(onClick = {}) { Text("OK") } },
            dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }) {
            DatePicker(state, showModeToggle = false)
        }
    }

    private fun shoot(name: String, content: @Composable () -> Unit) = runDesktopComposeUiTest(780, 1688, testTimeout = 8.minutes) { // railDark takes ~2 min on a fast desktop; CI runners are slower (26 Oct)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f)) {
                Box(Modifier.size(390.dp, 844.dp).testTag("page")) { content() }
            }
        }
        waitForIdle()
        ImageIO.write(onNodeWithTag("page").captureToImage().toAwtImage(), "png", File(out, "$name.png"))
    }

    @Test fun moreLight() = shoot("catalogue-more-light") { More(dark = false) }
    @Test fun moreDark() = shoot("catalogue-more-dark") { More(dark = true) }
    @Test fun drawerLight() = shoot("catalogue-drawer-light") { Drawer(dark = false) }
    @Test fun pickerLight() = shoot("catalogue-picker-light") { Picker(dark = false) }
    @Test fun controlsDark() = shoot("catalogue-controls-dark") { Controls(dark = true) }
    @Test fun controlsLight() = shoot("catalogue-controls-light") { Controls(dark = false) }
    @Test fun navigationDark() = shoot("catalogue-navigation-dark") { Navigation(dark = true) }
    @Test fun navigationLight() = shoot("catalogue-navigation-light") { Navigation(dark = false) }
    @Test fun railDark() = shoot("catalogue-rail-dark") { RailAndMore(dark = true) }
}
