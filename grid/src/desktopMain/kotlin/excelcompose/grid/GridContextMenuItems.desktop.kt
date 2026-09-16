package excelcompose.grid

import androidx.compose.foundation.ContextMenuDataProvider
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
actual fun GridContextMenuItems(items: () -> List<Pair<String, () -> Unit>>, content: @Composable () -> Unit) {
    ContextMenuDataProvider(
        items = { items().map { (label, onClick) -> ContextMenuItem(label, onClick) } },
        content = content,
    )
}
