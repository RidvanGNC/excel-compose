package excelcompose.grid

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun gridScrollbarStyle(): ScrollbarStyle {
    val ink = MaterialTheme.colorScheme.onSurface
    return ScrollbarStyle(
        minimalHeight = 24.dp,
        thickness = 10.dp,
        shape = RoundedCornerShape(5.dp),
        hoverDurationMillis = 300,
        unhoverColor = ink.copy(alpha = 0.32f),
        hoverColor = ink.copy(alpha = 0.58f),
    )
}

@Composable
actual fun GridVerticalScrollbar(listState: LazyListState, modifier: Modifier) {
    VerticalScrollbar(rememberScrollbarAdapter(listState), modifier, style = gridScrollbarStyle())
}

@Composable
actual fun GridHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) {
    HorizontalScrollbar(rememberScrollbarAdapter(scrollState), modifier, style = gridScrollbarStyle())
}
