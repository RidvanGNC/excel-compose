package excelcompose.grid

import androidx.compose.runtime.Composable

@Composable
actual fun GridContextMenuItems(items: () -> List<Pair<String, () -> Unit>>, content: @Composable () -> Unit) = content()
