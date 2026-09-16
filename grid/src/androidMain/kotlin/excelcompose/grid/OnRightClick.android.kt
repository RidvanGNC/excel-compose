package excelcompose.grid

import androidx.compose.ui.Modifier

actual fun Modifier.onRightClick(vararg keys: Any?, onRightClick: () -> Unit): Modifier = this
