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
    val lineColor: Color,
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
        lineColor: Color = MaterialTheme.colorScheme.outlineVariant,
    ): ExcelGridColors = ExcelGridColors(
        containerColor = containerColor,
        headerContainerColor = headerContainerColor,
        filterContainerColor = filterContainerColor,
        rowContainerColor = rowContainerColor,
        selectedRowContainerColor = selectedRowContainerColor,
        lineColor = lineColor,
    )
}
