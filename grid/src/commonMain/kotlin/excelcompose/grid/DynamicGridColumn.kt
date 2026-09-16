package excelcompose.grid

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

/**
 * A row whose shape isn't known at compile time — a plain map from column id to that
 * column's value. Used with [GridColumn.dynamic] for a grid whose columns are themselves
 * defined at runtime (e.g. by a user, in a host app that lets people build their own
 * tables), as opposed to the ordinary `GridColumn<T>` bound to a concrete class.
 */
typealias DynamicRow = Map<String, Any?>

/**
 * A [GridColumn] for a [DynamicRow]-typed grid — cell rendering and the filter-row box are
 * both picked automatically from [dataType], instead of the host app writing its own
 * [ExcelComposeCell]/[ExcelComposeFilter] per column: [ColumnDataType.Number] right-aligns;
 * [ColumnDataType.Boolean] renders [trueLabel]/[falseLabel] and gets a [allLabel]/[trueLabel]/
 * [falseLabel] filter dropdown instead of a text box; [ColumnDataType.Text]/[ColumnDataType.
 * Date] render their value as plain text (a host app owns formatting a [ColumnDataType.Date]
 * value into the row map however it likes — this only decides how it's *displayed*, not
 * parsed). A missing or wrong-typed value under [id] renders blank rather than throwing,
 * since a runtime-defined schema can't be checked at compile time the way `GridColumn<T>`'s
 * typed lambdas are.
 *
 * Example: `GridColumn.dynamic(id = "aktif", heading = "Aktif", width = 100.dp, dataType =
 * ColumnDataType.Boolean, trueLabel = "Evet", falseLabel = "Hayır", allLabel = "Hepsi")`.
 */
fun GridColumn.Companion.dynamic(
    id: String,
    heading: String,
    width: Dp,
    dataType: ColumnDataType,
    sortable: Boolean = false,
    /** Only used when [dataType] is [ColumnDataType.Boolean] — the "true"/"all" choices in
     * both the cell text and the filter dropdown. */
    trueLabel: String = "Yes",
    falseLabel: String = "No",
    allLabel: String = "All",
): GridColumn<DynamicRow> = GridColumn(
    id = id,
    heading = heading,
    width = width,
    cell = ExcelComposeCell.TextCell { row -> row[id].toDisplayText(dataType, trueLabel, falseLabel) },
    filter = if (dataType is ColumnDataType.Boolean) {
        ExcelComposeFilter.ChoiceFilter(listOf("" to allLabel, "true" to trueLabel, "false" to falseLabel))
    } else {
        ExcelComposeFilter.TextFilter
    },
    sortable = sortable,
    align = if (dataType is ColumnDataType.Number) TextAlign.End else TextAlign.Start,
)

private fun Any?.toDisplayText(dataType: ColumnDataType, trueLabel: String, falseLabel: String): String = when {
    this == null -> ""
    dataType is ColumnDataType.Boolean -> if (this as? Boolean == true) trueLabel else falseLabel
    else -> toString()
}
