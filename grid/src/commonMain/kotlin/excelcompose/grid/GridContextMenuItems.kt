package excelcompose.grid

import androidx.compose.runtime.Composable

/**
 * Wraps [content] with extra right-click context-menu items (desktop only) — each an
 * (label, onClick) pair, shown alongside whatever the platform already offers (e.g. "Copy"
 * for selected text). A no-op — just renders [content] as-is — when [items] returns an
 * empty list, or on any non-desktop target (there's no right-click there). [items] is only
 * invoked once the menu is actually opened, so it's cheap to recompute on every call.
 *
 * Example: `GridContextMenuItems({ listOf("Copy cell" to { clipboard.setText(...) }) }) {
 * Text(cellValue) }`.
 */
@Composable
expect fun GridContextMenuItems(items: () -> List<Pair<String, () -> Unit>>, content: @Composable () -> Unit)
