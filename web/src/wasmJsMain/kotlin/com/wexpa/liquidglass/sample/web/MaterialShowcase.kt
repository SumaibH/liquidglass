package com.wexpa.liquidglass.sample.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.material3.*

private val Covers = listOf(
    Color(0xFFFF5E62) to Color(0xFFFF9966), Color(0xFF4776E6) to Color(0xFF8E54E9),
    Color(0xFF11998E) to Color(0xFF38EF7D), Color(0xFFF7971E) to Color(0xFFFFD200),
    Color(0xFFEC008C) to Color(0xFFFC6767), Color(0xFF00C6FF) to Color(0xFF0072FF),
)
private val Titles = listOf("Midnight Drive", "Glasshouse", "Evergreen", "Golden Hour", "Neon Bloom", "Open Water")

/**
 * A Material 3 app built only from `liquidglass-material3`: the screen rendered for the README, live.
 * Everything is interactive: the tab bar, the segmented control, the switches, the slider, the
 * dialog, the sheet and the menu. The scheme follows the sun/moon button.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MaterialShowcase(onSwitchScreen: () -> Unit) {
    var dark by remember { mutableStateOf(true) }
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        var tab by remember { mutableIntStateOf(0) }
        var segment by remember { mutableIntStateOf(0) }
        var crossfade by remember { mutableStateOf(true) }
        var volume by remember { mutableFloatStateOf(.62f) }
        var dialog by remember { mutableStateOf(false) }
        var sheet by remember { mutableStateOf(false) }
        var menu by remember { mutableStateOf(false) }
        var downloaded by remember { mutableStateOf(true) }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Library") },
                    navigationIcon = { IconButton(onClick = onSwitchScreen) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atlas Studio") } },
                    actions = {
                        IconButton(onClick = { dark = !dark }) { Icon(if (dark) Icons.Default.Star else Icons.Default.Favorite, "Switch scheme") }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "More") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Show dialog") }, onClick = { menu = false; dialog = true })
                                DropdownMenuItem(text = { Text("Show sheet") }, onClick = { menu = false; sheet = true })
                                DropdownMenuItem(text = { Text("Atlas Studio") }, onClick = { menu = false; onSwitchScreen() })
                            }
                        }
                    },
                )
            },
            bottomBar = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NavigationBar(accessory = { Icon(Icons.Default.Search, "Search") }, onAccessoryClick = { sheet = true },
                    modifier = Modifier.widthIn(max = 520.dp)) {
                    listOf(Icons.Default.Home to "Home", Icons.Default.Favorite to "Favourites", Icons.Default.Person to "Profile")
                        .forEachIndexed { i, (icon, label) ->
                            NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, null) }, label = { Text(label) })
                        }
                }
            } },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    GridCells.Adaptive(160.dp), Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 260.dp, bottom = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(48) { i ->
                        val (from, to) = Covers[(i + tab) % Covers.size]
                        Column {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(from, to))))
                            Text(Titles[(i + tab) % Titles.size], Modifier.padding(top = 6.dp),
                                color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                Column(
                    Modifier.padding(padding).widthIn(max = 520.dp).fillMaxWidth().align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Albums", "Songs", "Artists").forEachIndexed { i, l ->
                            SegmentedButton(selected = segment == i, onClick = { segment = i },
                                shape = SegmentedButtonDefaults.itemShape(i, 3)) { Text(l) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { dialog = true }) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play") }
                        OutlinedButton(onClick = { sheet = true }) { Text("Shuffle") }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {}) { Icon(Icons.Default.Add, "Add") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = downloaded, onClick = { downloaded = !downloaded }, label = { Text("Downloaded") },
                            leadingIcon = if (downloaded) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null)
                        AssistChip(onClick = {}, label = { Text("Recently added") })
                    }
                }
                Card(Modifier.align(Alignment.BottomCenter).padding(padding).padding(16.dp).widthIn(max = 488.dp).fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Crossfade", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Switch(checked = crossfade, onCheckedChange = { crossfade = it })
                        }
                        Text("Volume", style = MaterialTheme.typography.bodyMedium)
                        Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (dialog) AlertDialog(
                    onDismissRequest = { dialog = false },
                    confirmButton = { TextButton(onClick = { dialog = false }) { Text("Play") } },
                    dismissButton = { TextButton(onClick = { dialog = false }) { Text("Cancel") } },
                    title = { Text("Play Midnight Drive?") },
                    text = { Text("The glass behind this dialog refracts the app, drawn in the same window.") },
                )
                if (sheet) ModalBottomSheet(onDismissRequest = { sheet = false }) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Up next", style = MaterialTheme.typography.titleLarge)
                        Titles.forEach { Text(it) }
                        Button(onClick = { sheet = false }) { Text("Done") }
                    }
                }
            }
        }
    }
}
