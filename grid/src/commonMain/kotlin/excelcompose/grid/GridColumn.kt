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
 */
data class GridColumn<T>(
    val id: String,
    val heading: String,
    val width: Dp,
    val filterable: Boolean = true,
    /** Clicking the header sorts by this column (ascending/descending). */
    val sortable: Boolean = false,
    val align: TextAlign = TextAlign.Start,
    val content: (@Composable (T) -> Unit)? = null,
    /** Custom filter row content (e.g. a dropdown); defaults to a plain text box. */
    val filterContent: (@Composable (current: String, onChange: (String) -> Unit) -> Unit)? = null,
    val value: (T) -> String,
)
