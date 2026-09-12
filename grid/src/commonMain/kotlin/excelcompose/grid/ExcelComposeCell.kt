package excelcompose.grid

import androidx.compose.runtime.Composable

/**
 * What a [GridColumn]'s cell body renders. A typed, discoverable alternative to a loose
 * `value: (T) -> String` + nullable `content: @Composable (T) -> Unit` pair — same spirit
 * as [ExcelComposeFilter]. Most columns don't need to reach for this directly: the
 * `GridColumn(..., value = { ... })` convenience constructor wraps [TextCell] for you.
 */
sealed interface ExcelComposeCell<T> {
    /** Plain text, rendered with the grid's default cell style. */
    data class TextCell<T>(val value: (T) -> String) : ExcelComposeCell<T>

    /** Escape hatch — any composable you write yourself (a switch, an icon, a multi-line cell). */
    data class CustomCell<T>(val content: @Composable (T) -> Unit) : ExcelComposeCell<T>
}
