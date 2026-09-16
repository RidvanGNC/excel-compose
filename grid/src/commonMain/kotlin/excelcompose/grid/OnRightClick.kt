package excelcompose.grid

import androidx.compose.ui.Modifier

/**
 * Desktop-only: calls [onRightClick] when this element receives a right-click (secondary
 * mouse button press) — [keys] behave exactly like [androidx.compose.ui.input.pointer.
 * pointerInput]'s own keys, restarting detection when any of them changes. A no-op Modifier
 * on any non-desktop target — touch has no secondary button there.
 */
expect fun Modifier.onRightClick(vararg keys: Any?, onRightClick: () -> Unit): Modifier
