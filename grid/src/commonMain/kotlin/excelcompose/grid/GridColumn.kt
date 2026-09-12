package excelcompose.grid

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp

/**
 * A single column definition for [DataGrid].
 *
 * @param width base (minimum) width; columns grow proportionally to fill extra
 * space when the grid is wider than the sum of all column widths.
 * @param content custom composable for the cell body, replacing the default text.
 * @param filter what the filter-row box looks like for this column — see [ExcelComposeFilter].
 */
data class GridColumn<T>(
    val id: String,
    val heading: String,
    val width: Dp,
    val filter: ExcelComposeFilter = ExcelComposeFilter.TextFilter,
    /** Clicking the header sorts by this column (ascending/descending). */
    val sortable: Boolean = false,
    val align: TextAlign = TextAlign.Start,
    val content: (@Composable (T) -> Unit)? = null,
    val value: (T) -> String,
)
