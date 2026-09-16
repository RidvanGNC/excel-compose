package excelcompose.grid

/**
 * How a [DataGrid] scrollbar (vertical or horizontal, configured independently) should
 * behave. Same shape as [ExcelComposeFilter]/[ExcelComposeCell] — a closed set of named
 * cases rather than a plain `Boolean` — even though none of these carry data, for one
 * consistent vocabulary across the library instead of mixing enums and sealed interfaces
 * for what is conceptually the same kind of "pick one of several named behaviors" choice.
 */
sealed interface ScrollbarVisibility {
    /** Never drawn. Scrolling itself (wheel/trackpad/drag) is unaffected — only the visible thumb is gone. */
    data object Hidden : ScrollbarVisibility

    /** Drawn only while there's actually something to scroll to in that direction. The default. */
    data object Overflow : ScrollbarVisibility

    /** Always drawn, even when the content already fits. */
    data object Visible : ScrollbarVisibility
}
