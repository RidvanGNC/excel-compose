package excelcompose.grid

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

/**
 * A single column definition for [DataGrid].
 *
 * @param width the column's on-screen width (or its starting width, if the user resizes it).
 * @param cell what the cell body renders — see [ExcelComposeCell]. Use the
 * `GridColumn(..., value = { ... })` overload below for the common plain-text case.
 * @param filter what the filter-row box looks like for this column — see [ExcelComposeFilter].
 * @param contentAlign how this column's BODY cells (not header/filter) position their content
 * within the cell's full row-height box — e.g. [Alignment.TopStart] to pin content to the top
 * of a taller-than-text row instead of the default vertical centering. Every 3x3 [Alignment]
 * combination is valid; [align] (a plain [TextAlign]) is unrelated and only ever affects a
 * [ExcelComposeCell.TextCell]'s own horizontal text alignment.
 */
@Immutable
data class GridColumn<T>(
    val id: String,
    val heading: String,
    val width: Dp,
    val cell: ExcelComposeCell<T>,
    val filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    /** Clicking the header sorts by this column (ascending/descending). */
    val sortable: Boolean = false,
    val align: TextAlign = TextAlign.Start,
    val contentAlign: Alignment = Alignment.CenterStart,
) {
    /** Anchors [dynamic] (see `DynamicGridColumn.kt`) — a factory for a grid whose columns
     * are defined at runtime (a [ColumnDataType] per column) rather than this file's own
     * compile-time-typed `GridColumn<T>`. */
    companion object
}

/** Convenience for the common case — a column that just renders plain text. */
fun <T> GridColumn(
    id: String,
    heading: String,
    width: Dp,
    filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    sortable: Boolean = false,
    align: TextAlign = TextAlign.Start,
    contentAlign: Alignment = Alignment.CenterStart,
    value: (T) -> String,
): GridColumn<T> = GridColumn(id, heading, width, ExcelComposeCell.TextCell(value), filter, sortable, align, contentAlign)
