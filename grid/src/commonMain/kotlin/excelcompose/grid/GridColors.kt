package excelcompose.grid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Explicit color parameters for [DataGrid] — no CompositionLocal/app-theme dependency,
 * same pattern as [androidx.compose.material3.Scaffold]'s `containerColor`: pass values in,
 * get sensible Material3-derived defaults via [ExcelGridDefaults.colors] when you don't.
 */
@Immutable
data class ExcelGridColors(
    /** Backdrop behind the whole grid — the single knob for "change the overall background". */
    val containerColor: Color,
    val headerContainerColor: Color,
    val filterContainerColor: Color,
    val rowContainerColor: Color,
    val selectedRowContainerColor: Color,
    /**
     * A [rowDimmed] row's background — deliberately a SEPARATE token from
     * [filterContainerColor], not a reuse of it. It used to borrow filterContainerColor,
     * which is semi-transparent (0.6 alpha) by design for its own actual purpose (a light
     * wash under the filter row); reused as a row background, that transparency let whatever
     * was underneath keep showing through, which is what read as a persistent "background
     * doesn't fully cover" bug through several rounds of otherwise-unrelated fixes. This is
     * fully opaque by default.
     */
    val dimmedRowContainerColor: Color,
    val lineColor: Color,
    /** Border around every filter box (text/[ChoiceFilterCell]/[MultiChoiceFilterCell] alike). */
    val filterBorderColor: Color,
    /** The single-block outline drawn around a column while it's being drag-reordered. */
    val selectedBorderColor: Color,
)

object ExcelGridDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        headerContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        filterContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        rowContainerColor: Color = MaterialTheme.colorScheme.surface,
        selectedRowContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        dimmedRowContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        lineColor: Color = MaterialTheme.colorScheme.outlineVariant,
        filterBorderColor: Color = MaterialTheme.colorScheme.outline,
        selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
    ): ExcelGridColors = ExcelGridColors(
        containerColor = containerColor,
        headerContainerColor = headerContainerColor,
        filterContainerColor = filterContainerColor,
        rowContainerColor = rowContainerColor,
        selectedRowContainerColor = selectedRowContainerColor,
        dimmedRowContainerColor = dimmedRowContainerColor,
        lineColor = lineColor,
        filterBorderColor = filterBorderColor,
        selectedBorderColor = selectedBorderColor,
    )
}
