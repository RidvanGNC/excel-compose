package excelcompose.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private val MIN_COLUMN_WIDTH = 40.dp
private val RESIZE_HANDLE_WIDTH = 10.dp
private val SELECTION_COLUMN_WIDTH = 44.dp

/** Exposed so a host app can size a container to an exact number of visible rows. */
val ROW_HEIGHT = 30.dp
val HEADER_HEIGHT = 32.dp
val FILTER_ROW_HEIGHT = 32.dp

/**
 * An Excel-style dense data grid: header + column-filter row + a lazily-laid-out body,
 * all sharing one horizontal scroll position. Columns stretch proportionally to fill
 * extra width when the grid is wider than the sum of column widths; otherwise the grid
 * scrolls horizontally. See [GridColumn] for per-column configuration and
 * [ExcelGridColors] for theming (no CompositionLocal — pass colors in directly, same
 * spirit as [androidx.compose.material3.Scaffold]'s `containerColor`).
 *
 * Column widths dragged by the user are kept in memory for the lifetime of this
 * composable; [initialColumnWidths] seeds them and [onColumnWidthChange] is called once a
 * drag ends so the host app can persist it however it likes (this library has no
 * opinion on storage).
 */
@Composable
fun <T> DataGrid(
    columns: List<GridColumn<T>>,
    rows: List<T>,
    key: (T) -> String,
    modifier: Modifier = Modifier,
    colors: ExcelGridColors = ExcelGridDefaults.colors(),
    /** Shows a leading checkbox column with a tri-state "select all" in the header. */
    selectable: Boolean = false,
    selectedKeys: Set<String> = emptySet(),
    onSelect: (key: String) -> Unit = {},
    onSelectAll: (selectAll: Boolean) -> Unit = {},
    filters: Map<String, String> = emptyMap(),
    onFilter: (columnId: String, value: String) -> Unit = { _, _ -> },
    /** Whether the column-filter row renders at all. When false, the row is omitted entirely
     * (not just hidden) and the body takes up that vertical space. */
    filterRowEnabled: Boolean = true,
    /** Color of the default filter-icon glyph — ignored if [filterTrailingIcon] is overridden. */
    filterIconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    /** Size of the default filter-icon glyph — ignored if [filterTrailingIcon] is overridden. */
    filterIconSize: Dp = 10.dp,
    /** Trailing glyph inside the default [ExcelComposeFilter.TextFilter] box — replace with your own icon. */
    filterTrailingIcon: @Composable () -> Unit = {
        FilterGlyph(tint = filterIconColor, modifier = Modifier.size(filterIconSize))
    },
    loading: Boolean = false,
    loadingNextPage: Boolean = false,
    onRowOpen: (T) -> Unit = {},
    /**
     * Escape hatch: fires on every row tap, alongside whatever [onSelect]/[onRowOpen]
     * already do — `isDoubleTap` tells single from double so a host app can layer its
     * own behavior (e.g. a context menu, a hover-like highlight) without forking the grid.
     */
    onRowTap: ((row: T, isDoubleTap: Boolean) -> Unit)? = null,
    /** Called once the body has scrolled within 8 rows of the end — hook up pagination here. */
    onNearEnd: () -> Unit = {},
    initialColumnWidths: Map<String, Dp> = emptyMap(),
    onColumnWidthChange: (columnId: String, width: Dp) -> Unit = { _, _ -> },
    /** Dimmed (55% alpha) rows — e.g. inactive records. */
    rowDimmed: ((T) -> Boolean)? = null,
    /** Non-null return tints that row's background (selection still wins visually). */
    rowTint: ((T) -> Color?)? = null,
    /** Single tap opens the row instead of the default double tap. */
    openOnSingleTap: Boolean = false,
    /**
     * Whether a lone tap should do anything at all — defaults to true only when the grid
     * actually reacts to one ([selectable] or [openOnSingleTap]). Override explicitly if
     * you rely purely on [onRowTap] for single-tap behavior and want it to fire.
     */
    singleTapEnabled: Boolean = selectable || openOnSingleTap,
    /** Whether a second tap should be recognized as a double-click at all. */
    doubleTapEnabled: Boolean = true,
    /**
     * How long a second tap has to arrive to count as a double-click. This is also the
     * price every SINGLE tap pays when [singleTapEnabled] AND [doubleTapEnabled] are both
     * true — no tap system can know a click is "final" until either this timeout passes or
     * a second tap fails to arrive, so needing both behaviors on the same row is the one
     * case with unavoidable latency. Grids that only select (single) or only open
     * (double) never wait at all — one of the two has nothing to disambiguate against.
     */
    doubleTapTimeoutMillis: Long = 300L,
    sortId: String? = null,
    sortDesc: Boolean = false,
    onSort: ((columnId: String) -> Unit)? = null,
    emptyState: @Composable () -> Unit = {
        Text("No records", color = MaterialTheme.colorScheme.onSurfaceVariant)
    },
) {
    val hScroll = rememberScrollState()
    val listState = rememberLazyListState()

    val selectionColumnWidth = if (selectable) SELECTION_COLUMN_WIDTH else 0.dp
    val allSelected = rows.isNotEmpty() && selectedKeys.containsAll(rows.map(key))

    val nearEnd by remember(rows) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            rows.isNotEmpty() && lastVisible >= rows.size - 8
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) onNearEnd() }

    // Committed (drag-finished) widths live for this composable's lifetime, seeded once
    // from `initialColumnWidths`; `dragWidths` holds the width of a column only WHILE it
    // is actively being dragged. Keeping these separate is what stops sibling columns
    // from jittering while one column is being resized — see the width-distribution
    // comment below.
    val committedWidths = remember { mutableStateMapOf<String, Dp>().apply { putAll(initialColumnWidths) } }
    val dragWidths = remember { mutableStateMapOf<String, Dp>() }
    val density = LocalDensity.current

    BoxWithConstraints(modifier.background(colors.containerColor)) {
        val available = maxWidth

        val committedOverrideWidths = remember(columns, committedWidths.toMap()) {
            columns.map { c -> committedWidths[c.id] }
        }
        val stretchableCols = columns.filterIndexed { i, _ -> committedOverrideWidths[i] == null }
        val stretchableBase = stretchableCols.fold(0.dp) { a, c -> a + c.width }
        val overrideTotal = committedOverrideWidths.filterNotNull().fold(0.dp) { a, w -> a + w }
        val baseTotal = selectionColumnWidth + overrideTotal + stretchableBase

        // The extra-space distribution pool only looks at COMMITTED widths, so it stays
        // fixed while a drag is in progress — otherwise every stretchable column would
        // visibly shrink in lockstep as the dragged column grows, since they'd all be
        // sharing a pool that shrinks with it. The live drag width is applied afterwards,
        // to that one column only.
        val effective = remember(columns, available, committedOverrideWidths, dragWidths.toMap()) {
            val settled = if (baseTotal < available && stretchableBase > 0.dp) {
                val extra = available - baseTotal
                columns.mapIndexed { i, c -> committedOverrideWidths[i] ?: (c.width + extra * (c.width / stretchableBase)) }
            } else {
                columns.mapIndexed { i, c -> committedOverrideWidths[i] ?: c.width }
            }
            columns.mapIndexed { i, c -> dragWidths[c.id] ?: settled[i] }
        }
        val lineWidths = listOf(selectionColumnWidth) + effective
        val totalWidth = lineWidths.fold(0.dp) { a, w -> a + w }

        // Absolute right-edge x of each column, used to center resize handles exactly on
        // the border line regardless of how the column got its current width.
        val columnBoundaries = remember(effective, selectionColumnWidth) {
            var acc = selectionColumnWidth
            effective.map { w -> acc += w; acc }
        }

        Column(Modifier.fillMaxSize()) {
            // Header row + resize-handle overlay. The handles are a SEPARATE layer drawn
            // after (i.e. on top of) the header row within the same Box, so the pixel
            // exactly on a column border always hits the handle, never the neighboring
            // column's header (sort click, etc).
            Box(Modifier.horizontalScroll(hScroll).width(totalWidth).height(HEADER_HEIGHT)) {
                Row(
                    Modifier.fillMaxSize()
                        .background(colors.headerContainerColor)
                        .drawBehind { gridLines(lineWidths, colors.lineColor) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectable) {
                        Box(
                            Modifier.width(SELECTION_COLUMN_WIDTH).fillMaxHeight()
                                .clickable { onSelectAll(!allSelected) },
                            contentAlignment = Alignment.Center,
                        ) {
                            TriStateCheckbox(
                                state = when {
                                    allSelected -> ToggleableState.On
                                    selectedKeys.isEmpty() -> ToggleableState.Off
                                    else -> ToggleableState.Indeterminate
                                },
                                onClick = null,
                                modifier = Modifier.scale(0.85f),
                            )
                        }
                    }
                    columns.forEachIndexed { i, c ->
                        val canSort = c.sortable && onSort != null
                        val active = sortId == c.id
                        Row(
                            Modifier.width(effective[i]).fillMaxHeight()
                                .then(if (canSort) Modifier.clickable { onSort(c.id) } else Modifier)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                c.heading,
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (canSort) {
                                Text(
                                    if (active) (if (sortDesc) "▾" else "▴") else "↕",
                                    Modifier.padding(start = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
                columns.forEachIndexed { i, c ->
                    ColumnResizeHandle(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = columnBoundaries[i] - RESIZE_HANDLE_WIDTH / 2)
                            .fillMaxHeight(),
                        startWidth = effective[i],
                        density = density,
                        onDrag = { next -> dragWidths[c.id] = next },
                        onDragEnd = { next ->
                            dragWidths[c.id] = next
                            committedWidths[c.id] = next
                            onColumnWidthChange(c.id, next)
                        },
                    )
                }
            }

            // filter row
            if (filterRowEnabled) {
                Row(
                    Modifier.horizontalScroll(hScroll).width(totalWidth)
                        .background(colors.filterContainerColor)
                        .height(FILTER_ROW_HEIGHT)
                        .drawBehind { gridLines(lineWidths, colors.lineColor) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectable) Box(Modifier.width(SELECTION_COLUMN_WIDTH))
                    columns.forEachIndexed { i, c ->
                        when (val f = c.filter) {
                            is ExcelComposeFilter.NoFilter -> Box(Modifier.width(effective[i]))

                            is ExcelComposeFilter.TextFilter -> Box(Modifier.width(effective[i]).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .border(1.dp, colors.filterBorderColor, RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    BasicTextField(
                                        value = filters[c.id].orEmpty(),
                                        onValueChange = { onFilter(c.id, it) },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f),
                                    )
                                    filterTrailingIcon()
                                }
                            }

                            is ExcelComposeFilter.ChoiceFilter -> Box(Modifier.width(effective[i]).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                ChoiceFilterCell(filters[c.id].orEmpty(), f.options, colors.filterBorderColor) { v -> onFilter(c.id, v) }
                            }

                            is ExcelComposeFilter.MultiChoiceFilter -> Box(Modifier.width(effective[i]).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                MultiChoiceFilterCell(filters[c.id].orEmpty(), f.options, f.allLabel, colors.filterBorderColor) { v -> onFilter(c.id, v) }
                            }

                            is ExcelComposeFilter.CustomFilter -> Box(Modifier.width(effective[i]).padding(horizontal = 6.dp, vertical = 3.dp)) {
                                f.content(filters[c.id].orEmpty()) { v -> onFilter(c.id, v) }
                            }
                        }
                    }
                }
            }

            // body
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SelectionContainer {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(rows, key = key) { row ->
                            val current = key(row) in selectedKeys
                            val dimmed = rowDimmed?.invoke(row) == true
                            val tint = rowTint?.invoke(row)
                            Row(
                                Modifier.horizontalScroll(hScroll).width(totalWidth).height(ROW_HEIGHT)
                                    .background(
                                        when {
                                            current -> colors.selectedRowContainerColor
                                            dimmed -> colors.filterContainerColor
                                            tint != null -> tint
                                            else -> colors.rowContainerColor
                                        },
                                    )
                                    .drawBehind { gridLines(lineWidths, colors.lineColor) }
                                    .rowTapGestures(
                                        singleTapEnabled = singleTapEnabled,
                                        doubleTapEnabled = doubleTapEnabled,
                                        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
                                        onTap = {
                                            when {
                                                openOnSingleTap -> onRowOpen(row)
                                                selectable -> onSelect(key(row))
                                            }
                                            onRowTap?.invoke(row, false)
                                        },
                                        onDoubleTap = {
                                            onRowOpen(row)
                                            onRowTap?.invoke(row, true)
                                        },
                                    )
                                    .alpha(if (dimmed) 0.55f else 1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selectable) {
                                    Box(
                                        Modifier.width(SELECTION_COLUMN_WIDTH).fillMaxHeight(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Checkbox(
                                            checked = current,
                                            onCheckedChange = null,
                                            modifier = Modifier.scale(0.85f),
                                        )
                                    }
                                }
                                columns.forEachIndexed { i, c ->
                                    Box(Modifier.width(effective[i]).padding(horizontal = 8.dp)) {
                                        when (val cell = c.cell) {
                                            is ExcelComposeCell.CustomCell -> cell.content(row)
                                            is ExcelComposeCell.TextCell -> Text(
                                                cell.value(row),
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = c.align,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (loadingNextPage) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }

                GridVerticalScrollbar(
                    listState,
                    Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 2.dp),
                )

                if (!loading && rows.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { emptyState() }
                }
            }

            GridHorizontalScrollbar(hScroll, Modifier.fillMaxWidth().padding(horizontal = 2.dp))
        }
    }
}

/**
 * Column-border resize handle (Excel-style). The visible line is 1dp but the touch/drag
 * target is [RESIZE_HANDLE_WIDTH] wide so it's easy to grab with a mouse or a finger.
 * [startWidth] is read fresh on every recomposition; [onDrag] fires on every pointer move,
 * [onDragEnd] fires once when the drag finishes (that's the point to persist it).
 */
@Composable
private fun ColumnResizeHandle(
    modifier: Modifier = Modifier,
    startWidth: Dp,
    density: Density,
    onDrag: (Dp) -> Unit,
    onDragEnd: (Dp) -> Unit,
) {
    val currentStartWidth by rememberUpdatedState(startWidth)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    var widthPx by remember { mutableStateOf(0f) }
    Box(
        modifier
            .width(RESIZE_HANDLE_WIDTH)
            .horizontalResizeCursor()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { widthPx = with(density) { currentStartWidth.toPx() } },
                    onDragEnd = { currentOnDragEnd(with(density) { widthPx.toDp() }) },
                    onDragCancel = { currentOnDragEnd(with(density) { widthPx.toDp() }) },
                ) { change, dragAmount ->
                    change.consume()
                    widthPx = (widthPx + dragAmount.x).coerceAtLeast(with(density) { MIN_COLUMN_WIDTH.toPx() })
                    currentOnDrag(with(density) { widthPx.toDp() })
                }
            },
    )
}

/**
 * Tap/double-tap detection whose latency depends on what's actually enabled — there is no
 * way to tell a single click from the first half of a double click without EITHER waiting
 * out [doubleTapTimeoutMillis] or accepting that a double click also fires [onTap] once
 * before [onDoubleTap] (see the DataGrid doc on [singleTapEnabled]/[doubleTapEnabled] for
 * why). So instead of always paying that cost, this only waits when it actually has
 * something to disambiguate:
 * - only [singleTapEnabled]: [onTap] fires the instant a tap-up is seen, no wait at all.
 * - only [doubleTapEnabled]: waits up to [doubleTapTimeoutMillis] for a second tap; a lone
 *   tap does nothing (there's no single-tap behavior to fire early), so no cost either.
 * - both enabled: every tap waits up to [doubleTapTimeoutMillis] to see if a second one
 *   arrives — [onTap] only fires once that window passes without one, [onDoubleTap] fires
 *   (instead, not in addition) the moment a second tap-up completes inside it. This is the
 *   one combination that pays real latency, because it's the one case where a click is
 *   genuinely ambiguous until proven otherwise.
 */
private fun Modifier.rowTapGestures(
    singleTapEnabled: Boolean,
    doubleTapEnabled: Boolean,
    doubleTapTimeoutMillis: Long,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
): Modifier {
    if (!singleTapEnabled && !doubleTapEnabled) return this
    return pointerInput(singleTapEnabled, doubleTapEnabled, doubleTapTimeoutMillis, onTap, onDoubleTap) {
        awaitEachGesture {
            awaitFirstDown()
            if (waitForUpOrCancellation() == null) return@awaitEachGesture // drag/scroll, not a tap

            if (!doubleTapEnabled) {
                onTap()
                return@awaitEachGesture
            }

            val secondDown = withTimeoutOrNull(doubleTapTimeoutMillis) { awaitFirstDown() }
            if (secondDown == null) {
                if (singleTapEnabled) onTap() // window passed with no second tap: a genuine single
                return@awaitEachGesture
            }
            if (waitForUpOrCancellation() != null) {
                onDoubleTap()
            } else if (singleTapEnabled) {
                onTap() // second press turned into a drag — fall back to treating the first as a single
            }
        }
    }
}

/**
 * Small funnel/filter glyph for the default text filter box — hand-drawn so the library
 * doesn't need to pull in a whole icon-font dependency for one shape.
 */
@Composable
private fun FilterGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w * 0.6f, h * 0.55f)
            lineTo(w * 0.6f, h)
            lineTo(w * 0.4f, h * 0.8f)
            lineTo(w * 0.4f, h * 0.55f)
            close()
        }
        drawPath(path, color = tint)
    }
}

/** Vertical lines at column borders, one horizontal line under the row/header (Excel look). */
private fun DrawScope.gridLines(columnWidths: List<Dp>, color: Color) {
    val thickness = 1.dp.toPx()
    var x = 0f
    columnWidths.forEach { w ->
        x += w.toPx()
        drawLine(color, Offset(x, 0f), Offset(x, size.height), thickness)
    }
    val y = size.height - thickness / 2f
    drawLine(color, Offset(0f, y), Offset(size.width, y), thickness)
}
