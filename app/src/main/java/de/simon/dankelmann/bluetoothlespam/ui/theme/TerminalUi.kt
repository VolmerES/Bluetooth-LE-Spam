package de.simon.dankelmann.bluetoothlespam.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable building blocks for the amber-CRT / terminal look (see Color.kt, Type.kt). Centralised
 * here so every screen renders the same console panels instead of re-styling Material Cards ad hoc.
 */

/**
 * Cheap, static CRT scanlines drawn over the content. No animation (the app has had UI-jank
 * issues, so this stays a single draw pass): just faint horizontal lines every [gap] px.
 */
fun Modifier.crtScanlines(
    color: Color = Color.Black.copy(alpha = 0.10f),
    gap: Dp = 3.dp,
): Modifier = this.drawWithContent {
    drawContent()
    val step = gap.toPx().coerceAtLeast(2f)
    var y = 0f
    while (y < size.height) {
        drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

/**
 * A boxed console panel: thin outline, dark surface, and an optional "> TITLE" header bar. Use
 * [accent] to outline it in the primary amber (for things that need attention).
 */
@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cs.surfaceContainerLow, SpecterShapes.medium)
            .border(1.dp, if (accent) cs.primary else cs.outline, SpecterShapes.medium),
    ) {
        if (title != null) {
            Text(
                text = "> ${title.uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = cs.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(cs.outlineVariant),
            )
        }
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

/** A `key            value` readout row, in the terminal palette. */
@Composable
fun TerminalKeyValue(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor ?: cs.onSurface,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd),
        )
    }
}

/** A flat, sharp-cornered amber button that reads like a shell command. */
@Composable
fun TerminalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = SpecterShapes.small,
        color = cs.primary,
        contentColor = cs.onPrimary,
        modifier = modifier.border(1.dp, cs.primary, SpecterShapes.small),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
