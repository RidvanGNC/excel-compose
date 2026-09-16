package excelcompose.grid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * What a [GridColumn]'s cell body renders. A typed, discoverable alternative to a loose
 * `value: (T) -> String` + nullable `content: @Composable (T) -> Unit` pair — same spirit
 * as [ExcelComposeFilter]. Most columns don't need to reach for this directly: the
 * `GridColumn(..., value = { ... })` convenience constructor wraps [TextCell] for you.
 */
@Immutable
sealed interface ExcelComposeCell<T> {
    /** Plain text, rendered with the grid's default cell style. */
    data class TextCell<T>(val value: (T) -> String) : ExcelComposeCell<T>

    /**
     * Escape hatch — any composable you write yourself (a switch, an icon, a multi-line
     * cell). [copyValue], if supplied, is this cell's plain-text stand-in for [DataGrid]'s
     * "copy cell"/"copy row" context-menu items (see `copyCellLabel`/`copyRowLabel`) — left
     * `null` (the default), a cell built this way is simply skipped by both, the same as a
     * column with no [copyValue] configured at all.
     */
    data class CustomCell<T>(val content: @Composable (T) -> Unit, val copyValue: ((T) -> String)? = null) : ExcelComposeCell<T>
}

/**
 * This cell's plain-text stand-in for [row], if any — a [TextCell] always has one; a
 * [CustomCell] only when its own [CustomCell.copyValue] was supplied. Used by [DataGrid]'s
 * "copy cell"/"copy row" context-menu items.
 */
fun <T> ExcelComposeCell<T>.copyText(row: T): String? = when (this) {
    is ExcelComposeCell.TextCell -> value(row)
    is ExcelComposeCell.CustomCell -> copyValue?.invoke(row)
}
