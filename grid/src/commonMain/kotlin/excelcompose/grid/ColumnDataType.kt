package excelcompose.grid

import androidx.compose.runtime.Immutable

/**
 * A column's declared data type — the vocabulary [GridColumn.dynamic] uses to pick sensible
 * default cell/filter rendering for a runtime-defined column, without the host app writing
 * its own `when` per column. Same spirit as [ExcelComposeFilter]/[ExcelComposeCell]: a
 * closed set of named cases, not a raw string or int.
 */
@Immutable
sealed interface ColumnDataType {
    data object Text : ColumnDataType
    data object Number : ColumnDataType
    data object Date : ColumnDataType
    data object Boolean : ColumnDataType
}
