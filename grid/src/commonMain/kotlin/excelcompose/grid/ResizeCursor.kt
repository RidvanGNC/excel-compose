package excelcompose.grid

import androidx.compose.ui.Modifier

/** Horizontal resize (⟷) mouse cursor on hover — desktop only, no-op elsewhere. */
expect fun Modifier.horizontalResizeCursor(): Modifier
