package excelcompose.grid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * What a column's filter-row box looks like. A typed, discoverable alternative to a loose
 * `filterable: Boolean` + nullable `@Composable` lambda pair — pick a named case instead of
 * wiring the composable yourself, unless you need [CustomFilter].
 */
@Immutable
sealed interface ExcelComposeFilter {
    /** No box at all for this column — the filter row just leaves its width empty. */
    data object NoFilter : ExcelComposeFilter

    /** A plain text box — the default when a [GridColumn] doesn't specify a filter. */
    data object TextFilter : ExcelComposeFilter

    /** Single-choice dropdown ([ChoiceFilterCell]). [options] are (value, label) pairs; the first means "all". */
    data class ChoiceFilter(val options: List<Pair<String, String>>) : ExcelComposeFilter

    /** Multi-choice dropdown ([MultiChoiceFilterCell]); selection is carried as a comma-separated string. */
    data class MultiChoiceFilter(val options: List<Pair<String, String>>, val allLabel: String) : ExcelComposeFilter

    /** Escape hatch — any composable you write yourself. */
    data class CustomFilter(
        val content: @Composable (current: String, onChange: (String) -> Unit) -> Unit,
    ) : ExcelComposeFilter
}
