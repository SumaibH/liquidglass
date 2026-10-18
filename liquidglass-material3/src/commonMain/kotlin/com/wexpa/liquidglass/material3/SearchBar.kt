package com.wexpa.liquidglass.material3

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wexpa.liquidglass.CapsuleShape
import com.wexpa.liquidglass.Glass
import com.wexpa.liquidglass.glassEffect

/**
 * A search bar as iOS 26 draws it: a glass capsule field, and below it, while [active], a glass panel
 * with [content] (suggestions, results). Material 3's classic `SearchBar` parameters.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).glassEffect(glass, shape).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = .7f)) { leadingIcon?.invoke() }
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty() && placeholder != null) {
                    CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = .5f)) {
                        ProvideTextStyle(MaterialTheme.typography.bodyLarge) { placeholder() }
                    }
                }
                BasicTextField(
                    value = query, onValueChange = onQueryChange, enabled = enabled, singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) onActiveChange(true) },
                )
            }
            if (trailingIcon != null) Spacer(Modifier.width(8.dp))
            CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = .7f)) { trailingIcon?.invoke() }
        }
        if (active) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().glassEffect(glass, RoundedCornerShape(26.dp)).padding(vertical = 8.dp)) {
                CompositionLocalProvider(LocalContentColor provides ink) { content() }
            }
        }
    }
}

/** The docked search bar: the same glass field and panel, inline in the layout. */
@Composable
fun DockedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CapsuleShape,
    glass: Glass = GlassMaterialDefaults.regular,
    content: @Composable ColumnScope.() -> Unit = {},
) = SearchBar(query, onQueryChange, onSearch, active, onActiveChange, modifier, enabled, placeholder, leadingIcon,
    trailingIcon, shape, glass, content)
