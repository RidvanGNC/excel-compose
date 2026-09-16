package excelcompose.grid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

private val MIN_COLUMN_WIDTH = 40.dp
private val RESIZE_HANDLE_WIDTH = 10.dp
private val SELECTION_COLUMN_WIDTH = 44.dp
private val ADD_COLUMN_RESERVED_WIDTH = 32.dp
private val FilterCellShape = RoundedCornerShape(4.dp)

/** Exposed so a host app can size a container to an exact number of visible rows. */
val ROW_HEIGHT = 30.dp
val HEADER_HEIGHT = 32.dp
val FILTER_ROW_HEIGHT = 32.dp

/**
 * An Excel-style dense data grid: header + column-filter row + a lazily-laid-out body,
 * all sharing one horizontal scroll position. Columns render at their configured (or
 * user-resized) width regardless of how wide the grid itself is — a grid wider than its
 * columns' combined width just shows plain background past the last one, rather than every
 * column stretching to cover it; a grid narrower than that sum scrolls horizontally instead.
 * See [GridColumn] for per-column configuration and [ExcelGridColors] for theming (no
 * CompositionLocal — pass colors in directly, same spirit as
 * [androidx.compose.material3.Scaffold]'s `containerColor`).
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
    /**
     * Desktop-only: adds "copy this cell's text" to the right-click context menu (which
     * otherwise just shows "Copy" for any actively selected text) — pass the label to show
     * (e.g. `"Copy cell"`, `"Hücreyi kopyala"`). `null` (the default) leaves it out
     * entirely, at no extra cost. Uses [ExcelComposeCell.copyText] — always available for
     * [ExcelComposeCell.TextCell], only for a [ExcelComposeCell.CustomCell] that supplies
     * its own `copyValue`. No effect on Android or any other non-desktop target — there's
     * no right-click there.
     */
    copyCellLabel: String? = null,
    /**
     * Desktop-only: adds "copy this whole row" to the right-click context menu — a heading
     * line (every copyable column's [GridColumn.heading]) followed by that row's own line
     * (each column's [ExcelComposeCell.copyText]), comma-separated, skipping any column with
     * no copyText (an [ExcelComposeCell.CustomCell] with no `copyValue`). `null` (the
     * default) leaves it out entirely. No effect on Android or any other non-desktop target.
     */
    copyRowLabel: String? = null,
    /**
     * Desktop-only: adds "copy every selected row" to the right-click context menu — same
     * heading line as [copyRowLabel], then one comma-separated line per currently selected
     * row (in `rows` order), each on its own line. Only offered while [selectable] and at
     * least one row is actually selected. `null` (the default) leaves it out entirely. No
     * effect on Android or any other non-desktop target.
     */
    copySelectedRowsLabel: String? = null,
    /** Called once the body has scrolled within 8 rows of the end — hook up pagination here. */
    onNearEnd: () -> Unit = {},
    initialColumnWidths: Map<String, Dp> = emptyMap(),
    onColumnWidthChange: (columnId: String, width: Dp) -> Unit = { _, _ -> },
    /**
     * Turns on column-editing affordances: a "+" button that appends a column
     * ([onAddColumn]), a delete glyph on each header ([onDeleteColumn]), and
     * long-press-drag column reordering ([onReorder]). Each affordance only renders when
     * its own callback is non-null, so a host app opts into exactly the subset it has
     * wired up — `editable = true` alone turns nothing on by itself.
     */
    editable: Boolean = false,
    /**
     * "+" button pinned to the header's top-right corner, always visible regardless of
     * horizontal scroll position. Fires with no arguments — the grid can't synthesize a
     * new column's shape itself (cells are typed to [T]), so the host app decides what to
     * append to its own `columns` list, the same way [onColumnWidthChange] already leaves
     * persistence entirely up to the caller.
     */
    onAddColumn: (() -> Unit)? = null,
    /** Delete glyph on each column header, shown only while [editable]. */
    onDeleteColumn: ((columnId: String) -> Unit)? = null,
    /**
     * Fired once a long-press-drag reorder completes — never mid-drag. [toIndex] is where
     * [fromIndex] should land AFTER removal, i.e.
     * `columns.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }` reproduces the
     * intended order.
     */
    onReorder: ((fromIndex: Int, toIndex: Int) -> Unit)? = null,
    /** Whether a sibling column animates smoothly into its new slot while reordering, vs
     * snapping there instantly. The grabbed column itself always tracks the pointer directly
     * either way — this only affects the OTHER columns reflowing to make room for it. */
    animateColumnReorder: Boolean = true,
    /** Duration of that reflow animation when [animateColumnReorder] is true. Ignored otherwise. */
    columnReorderAnimationMillis: Int = 180,
    verticalScrollbar: ScrollbarVisibility = ScrollbarVisibility.Overflow,
    horizontalScrollbar: ScrollbarVisibility = ScrollbarVisibility.Overflow,
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
    // from jittering while one column is being resized: `effective` below always prefers
    // a column's own dragWidths entry, so only the column actually under the pointer
    // recomputes its width text every frame — the others just read their settled value.
    val committedWidths = remember { mutableStateMapOf<String, Dp>().apply { putAll(initialColumnWidths) } }
    val dragWidths = remember { mutableStateMapOf<String, Dp>() }
    val density = LocalDensity.current

    // Column-reorder drag state — a single index + a single px offset, NOT anything
    // keyed per-column. Only the dragged header reads dragOffsetX (via graphicsLayer, a
    // draw-phase-only change that skips composition/layout), so moving it costs one
    // redraw of one layer per frame, not a recomposition of the grid. columns/effective
    // are untouched until onReorder fires on drop — see the header loop below.
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    // Declared once and reused by every columnCell() call (header, filter, every visible
    // body row) instead of each site allocating its own `{ dragIndex }`/`{ dragOffsetX }`
    // lambda — the lambdas themselves never change identity, and each still re-reads the
    // CURRENT state value when invoked (delegated-property reads, not a captured snapshot),
    // so this is a pure allocation savings, not a behavior change.
    val dragIndexOf: () -> Int? = { dragIndex }
    val dragOffsetXOf: () -> Float = { dragOffsetX }

    // One Animatable PER COLUMN (keyed by id), shared by the header cell, the filter cell,
    // and every visible body row's cell for that column — so they all animate the sibling-
    // shift reflow in perfect sync off ONE clock, instead of each row racing its own. Only
    // the header loop below drives these (LaunchedEffect); filter/body just read .value.
    val columnShift = remember { mutableStateMapOf<String, Animatable<Float, AnimationVector1D>>() }

    // Deleting a column (onDeleteColumn) never removes that column's id from these three
    // per-column maps on its own — they're keyed by id and nothing else prunes them. Without
    // this, every column ever added-then-deleted leaves a permanently orphaned Animatable/Dp
    // entry behind for the composable's whole lifetime. Reordering leaves the id set
    // unchanged, so this is a no-op on every recomposition except an actual add/delete.
    LaunchedEffect(columns) {
        val liveIds = columns.mapTo(mutableSetOf()) { it.id }
        columnShift.keys.retainAll(liveIds)
        committedWidths.keys.retainAll(liveIds)
        dragWidths.keys.retainAll(liveIds)
    }

    // clipToBounds so the selection overlay border (a plain sibling positioned by raw pixel
    // offset, not inside any scrolling/clipped child — see its own comment near the bottom
    // of this function) can never paint past this composable's own bounds while a column is
    // being dragged near the edge of a grid narrower than its total column width.
    BoxWithConstraints(modifier.background(colors.containerColor).clipToBounds()) {
        val committedOverrideWidths = remember(columns, committedWidths.toMap()) {
            columns.map { c -> committedWidths[c.id] }
        }

        // Columns render at their configured (or user-resized) width, full stop — they don't
        // stretch to fill extra space when the grid is wider than their sum. A grid wider
        // than its columns just shows plain background past the last one.
        val effective = remember(columns, committedOverrideWidths, dragWidths.toMap()) {
            val settled = columns.mapIndexed { i, c -> committedOverrideWidths[i] ?: c.width }
            columns.mapIndexed { i, c -> dragWidths[c.id] ?: settled[i] }
        }
        val lineWidths = listOf(selectionColumnWidth) + effective
        val totalWidth = lineWidths.fold(0.dp) { a, w -> a + w }

        // Where the dragged column would land if dropped RIGHT NOW — a derivedStateOf, not a
        // plain val, so reading it (composition-time, safe here — see the header loop) only
        // invalidates readers when the RESOLVED INDEX actually changes (a handful of times
        // per drag gesture), not on every pixel dragOffsetX moves. That's what makes it safe
        // to read at composition time to drive animateTo() targets below, unlike dragOffsetX
        // itself, which must stay confined to draw-phase reads (graphicsLayer/drawBehind).
        //
        // Keyed on effective/selectionColumnWidth: dragIndex/dragOffsetX are snapshot State
        // reads INSIDE the lambda, so derivedStateOf already recomputes on its own whenever
        // those change — but effective is a plain local List (a new instance whenever
        // columns/widths change), not something derivedStateOf observes by itself, so the
        // remember block itself needs to be re-run (producing a fresh derivedStateOf closed
        // over the current list) whenever effective's identity changes.
        val hoverIndex by remember(effective, selectionColumnWidth) {
            derivedStateOf {
                val from = dragIndex ?: return@derivedStateOf null
                resolveDropTarget(from, dragOffsetX, effective, selectionColumnWidth, density)
            }
        }

        // The "+" button isn't a real column, so it's deliberately left out of totalWidth/
        // lineWidths (used for cell layout and grid-line drawing). hScroll's shared maxValue
        // is derived from whatever width is passed to .horizontalScroll(hScroll)'s content,
        // which is entirely separate from where the button is drawn — reserving this width in
        // the SAME scrollable content that header/filter/body rows all share (scrollContentWidth
        // below) is what lets a scroll gesture actually reach the button on a grid narrower
        // than its total column width.
        val addColumn = if (editable) onAddColumn else null
        val scrollContentWidth = totalWidth + (if (addColumn != null) ADD_COLUMN_RESERVED_WIDTH else 0.dp)

        // Absolute right-edge x of each column, used to center resize handles exactly on
        // the border line regardless of how the column got its current width.
        val columnBoundaries = remember(effective, selectionColumnWidth) {
            var acc = selectionColumnWidth
            effective.map { w -> acc += w; acc }
        }

        Column(Modifier.fillMaxSize()) {
            // Outer static Box: the header itself scrolls horizontally, but the "+" button
            // (when editable) is a SIBLING that tracks the END of the actual column content —
            // computed from totalWidth and the live hScroll position, not pinned to the
            // container's own right edge, so it stays right after the last column even on a
            // grid wider than the columns' combined width. clipToBounds keeps it from
            // bleeding into the filter row below when it's positioned off the (unscrolled)
            // visible area on a grid wide enough to scroll.
            val totalWidthPx = with(density) { totalWidth.toPx() }
            Box(Modifier.fillMaxWidth().height(HEADER_HEIGHT).clipToBounds()) {
                // Header row + resize-handle overlay. The handles are a SEPARATE layer drawn
                // after (i.e. on top of) the header row within the same Box, so the pixel
                // exactly on a column border always hits the handle, never the neighboring
                // column's header (sort click, etc).
                Box(Modifier.horizontalScroll(hScroll).width(scrollContentWidth).fillMaxHeight()) {
                    Row(
                        // No shared background/gridlines here — every column cell paints its
                        // own (see columnCell's doc for why); this Row keeps only the bottom
                        // separator + selection-column boundary, which don't move during a
                        // column drag since they don't depend on DATA column order.
                        Modifier.fillMaxHeight().width(totalWidth)
                            .drawBehind { staticRowLines(colors.lineColor, selectionColumnWidth) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selectable) {
                            Box(
                                Modifier.width(SELECTION_COLUMN_WIDTH).fillMaxHeight()
                                    .background(colors.headerContainerColor)
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
                            // Keyed by column id, NOT by loop position: a plain forEachIndexed
                            // positionally matches slot N in this composition to slot N in the
                            // last one, so after a reorder the pointerInput/rememberUpdatedState
                            // below could otherwise get associated with the WRONG column (the
                            // one now sitting at that position) instead of following the column
                            // it actually belongs to.
                            key(c.id) {
                                val canSort = c.sortable && onSort != null
                                val active = sortId == c.id
                                // Only the reorder callback, gated on editable, turns the drag
                                // gesture on at all — with neither set this Modifier chain is
                                // byte-for-byte what it was before, no new gesture detector.
                                // rememberUpdatedState because the pointerInput coroutine below
                                // is keyed on c.id alone (see its own comment) and stays alive
                                // across recompositions where onReorder itself is a fresh lambda
                                // instance (e.g. a host app passing one inline) — without this,
                                // its onDragEnd would keep calling whatever onReorder was bound
                                // at the moment the coroutine first launched, not the latest one.
                                val currentReorder by rememberUpdatedState(onReorder)

                                // The one Animatable for this column, shared with its filter/
                                // body cells (see `columnShift` at the top of DataGrid). This
                                // header loop is the ONLY place that drives it — filter/body
                                // just read .value — because it's the one place guaranteed to
                                // be composed exactly once per column regardless of scroll
                                // position (LazyColumn body rows come and go; the header never
                                // does). dragIndex/hoverIndex both change rarely enough (a
                                // handful of times per drag gesture, not per pixel) that
                                // reading them here, at composition time, is safe — unlike
                                // dragOffsetX, which must stay confined to draw-phase reads.
                                val shiftAnim = remember(c.id) { columnShift.shiftAnimatable(c.id) }
                                LaunchedEffect(editable, dragIndex, hoverIndex, animateColumnReorder, columnReorderAnimationMillis) {
                                    val from = dragIndex
                                    if (!editable || from == null || i == from) {
                                        // Nothing to animate for the grabbed column itself
                                        // (it tracks the pointer directly, see columnCell) or
                                        // once a drag ends/cancels — snap instantly rather
                                        // than animate back, since by the time dragIndex clears
                                        // the underlying column order (if it changed) already
                                        // reflects the final layout; animating here would lag
                                        // visibly behind data that already moved.
                                        shiftAnim.snapTo(0f)
                                    } else {
                                        val hover = hoverIndex ?: from
                                        val draggedWidthPx = with(density) { effective[from].toPx() }
                                        val target = siblingShiftTarget(i, from, hover, draggedWidthPx)
                                        if (animateColumnReorder) {
                                            shiftAnim.animateTo(target, tween(columnReorderAnimationMillis))
                                        } else {
                                            shiftAnim.snapTo(target)
                                        }
                                    }
                                }
                                // detectDragGesturesAfterLongPress's block runs inside a
                                // long-lived coroutine keyed on c.id (see pointerInput below) —
                                // it is NOT relaunched just because `i`/`effective` changed on a
                                // later recomposition (a completed reorder, say), so it would
                                // otherwise keep reading the values captured when the drag
                                // FIRST started. rememberUpdatedState is the standard fix (same
                                // pattern ColumnResizeHandle already uses for startWidth below).
                                val currentIndex by rememberUpdatedState(i)
                                val currentEffective by rememberUpdatedState(effective)
                                val currentSelectionColumnWidth by rememberUpdatedState(selectionColumnWidth)
                                Row(
                                    Modifier.width(effective[i]).fillMaxHeight()
                                        // Shared with the filter cell and every visible body
                                        // row's cell for this same column (columnCell) — the
                                        // whole column, header down to values, moves as one
                                        // piece, background included, instead of a label
                                        // sliding over a backdrop it leaves behind.
                                        .columnCell(
                                            i,
                                            // Own color while dragging too, same as body cells
                                            // — headerContainerColor is already fully opaque
                                            // (no alpha reduction by default), so no separate
                                            // opaque variant is needed the way dimmed rows
                                            // needed one.
                                            backgroundColor = colors.headerContainerColor,
                                            lineColor = colors.lineColor,
                                            zIndexValue = columnZIndex(i, dragIndex, hoverIndex),
                                            editable = editable,
                                            dragIndexOf = dragIndexOf, dragOffsetXOf = dragOffsetXOf, shiftValueOf = { shiftAnim.value },
                                        )
                                        .then(if (canSort) Modifier.clickable { onSort(c.id) } else Modifier)
                                        .then(
                                            if (editable && currentReorder != null) {
                                                Modifier.pointerInput(c.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { dragIndex = currentIndex; dragOffsetX = 0f },
                                                        onDragEnd = {
                                                            val from = dragIndex
                                                            if (from != null) {
                                                                val to = resolveDropTarget(
                                                                    from, dragOffsetX, currentEffective,
                                                                    currentSelectionColumnWidth, this,
                                                                )
                                                                if (to != from) currentReorder?.invoke(from, to)
                                                            }
                                                            dragIndex = null
                                                            dragOffsetX = 0f
                                                        },
                                                        onDragCancel = { dragIndex = null; dragOffsetX = 0f },
                                                    ) { change, dragAmount ->
                                                        change.consume()
                                                        val from = dragIndex ?: return@detectDragGesturesAfterLongPress
                                                        // Clamp so the dragged column's drawn
                                                        // left edge can never leave the grid's own
                                                        // column area — without this, dragging
                                                        // past the last column pushes it (and,
                                                        // via the sibling-shift math above, other
                                                        // columns reacting to an out-of-range
                                                        // hover) beyond the grid entirely.
                                                        val leftEdgePx = columnLeftEdgePx(from, currentEffective, currentSelectionColumnWidth, this)
                                                        val draggedWidthPx = with(this) { currentEffective[from].toPx() }
                                                        val totalWidthPx = columnLeftEdgePx(currentEffective.size, currentEffective, currentSelectionColumnWidth, this)
                                                        val minOffset = currentSelectionColumnWidth.toPx() - leftEdgePx
                                                        val maxOffset = (totalWidthPx - draggedWidthPx) - leftEdgePx
                                                        dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(minOffset, maxOffset)
                                                    }
                                                }
                                            } else Modifier,
                                        )
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
                                    if (editable) {
                                        val delete = onDeleteColumn
                                        if (delete != null) {
                                            Text(
                                                "×",
                                                Modifier.padding(start = 4.dp).clickable { delete(c.id) },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    columns.forEachIndexed { i, c ->
                        key(c.id) {
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
                }

                if (addColumn != null) {
                    AddColumnButton(
                        onClick = addColumn,
                        // Tracks the current on-screen x of the END of the column content
                        // (totalWidthPx, shifted left as the header scrolls) rather than a
                        // fixed alignment — a lambda-based offset reads hScroll.value at
                        // layout time only, so scrolling doesn't recompose this Row.
                        modifier = Modifier.offset { IntOffset(x = (totalWidthPx - hScroll.value).roundToInt(), y = 0) },
                        colors = colors,
                    )
                }
            }

            // filter row
            if (filterRowEnabled) {
                Row(
                    // Wider than its own content (scrollContentWidth, not totalWidth) ONLY so
                    // hScroll's shared maxValue — computed from whichever of header/filter/body
                    // last measures — matches the header's, letting a scroll gesture actually
                    // reach the "+" button; see scrollContentWidth's comment. That's a LAYOUT
                    // concern only, though — what gets PAINTED (the background wash below, the
                    // bottom line) is bounded to totalWidthPx, not this Row's own (wider)
                    // measured width, so the "+" button's reserved space — which has no column
                    // of its own to own a background/line — stays visually empty. The base
                    // background here fills the true leftover margin (past the last real
                    // column, short of the reserved button space) plus the selection-column
                    // box below; every DATA column paints its own matching background on top
                    // of this and carries it along during a drag — see columnCell's doc.
                    // Vertical dividers between DATA columns are likewise each column's own
                    // responsibility — staticRowLines only draws the bottom separator and the
                    // (order-independent) selection-column boundary.
                    Modifier.horizontalScroll(hScroll).width(scrollContentWidth)
                        .drawBehind { drawRect(colors.filterContainerColor, size = Size(totalWidthPx, size.height)) }
                        .height(FILTER_ROW_HEIGHT)
                        .drawBehind { staticRowLines(colors.lineColor, selectionColumnWidth, totalWidthPx) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectable) Box(Modifier.width(SELECTION_COLUMN_WIDTH))
                    columns.forEachIndexed { i, c ->
                        // Same self-contained cell treatment as the header/body cells for this
                        // column (columnCell) — own background, own boundary line, own
                        // reorder-drag translation when editable — so the filter row doesn't
                        // sit still (visually disconnected) while the row above and below it
                        // are mid-reorder. Reads the SAME Animatable the header loop drives
                        // (see `columnShift`'s doc), not an independent animation of its own.
                        val shiftAnim = remember(c.id) { columnShift.shiftAnimatable(c.id) }
                        // Transparent for every filter cell except the one actively being
                        // dragged. All filter cells share the SAME uniform filterContainerColor
                        // wash this parent Row already paints once above; none of them ever
                        // differs from its neighbor's, so a static (non-dragging) cell has
                        // nothing of its own worth carrying — painting one anyway would just
                        // double that wash into a visibly stronger, blockier gray.
                        //
                        // The actively dragged cell is different: its translationX moves its
                        // content away from the row's static wash, which never moves (one
                        // shared layer, painted once, at a fixed position) — so only this one
                        // cell needs (and gets) its own opaque backing that travels WITH it,
                        // via the same graphicsLayer translation as everything else it carries.
                        val opaqueFilterColor = colors.filterContainerColor.compositeOver(colors.containerColor)
                        val filterDragging = editable && dragIndex == i
                        // fillMaxHeight so this cell matches the full FILTER_ROW_HEIGHT, not
                        // just its own content height.
                        val filterCellModifier = Modifier.width(effective[i]).fillMaxHeight().columnCell(
                            i,
                            backgroundColor = if (filterDragging) opaqueFilterColor else Color.Transparent,
                            lineColor = colors.lineColor,
                            zIndexValue = columnZIndex(i, dragIndex, hoverIndex),
                            editable = editable,
                            dragIndexOf = dragIndexOf, dragOffsetXOf = dragOffsetXOf, shiftValueOf = { shiftAnim.value },
                        )
                        val f = c.filter
                        Box(
                            filterCellModifier.then(
                                if (f is ExcelComposeFilter.NoFilter) Modifier else Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            ),
                        ) {
                            when (f) {
                                is ExcelComposeFilter.NoFilter -> Unit

                                is ExcelComposeFilter.TextFilter -> Row(
                                    Modifier.fillMaxWidth()
                                        .border(1.dp, colors.filterBorderColor, FilterCellShape)
                                        .background(MaterialTheme.colorScheme.surface, FilterCellShape)
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

                                is ExcelComposeFilter.ChoiceFilter ->
                                    ChoiceFilterCell(filters[c.id].orEmpty(), f.options, colors.filterBorderColor) { v -> onFilter(c.id, v) }

                                is ExcelComposeFilter.MultiChoiceFilter ->
                                    MultiChoiceFilterCell(filters[c.id].orEmpty(), f.options, f.allLabel, colors.filterBorderColor) { v -> onFilter(c.id, v) }

                                is ExcelComposeFilter.CustomFilter -> f.content(filters[c.id].orEmpty()) { v -> onFilter(c.id, v) }
                            }
                        }
                    }
                }
            }

            // body
            // Right-clicked row/cell (desktop only — see the per-cell pointerInput below for
            // why this lives here instead of a plain local val), read by GridContextMenuItems
            // just below to build "Copy cell"/"Copy row" — reading it happens lazily, only
            // once the context menu is actually opened, so writing it on a right-click never
            // recomposes anything (nothing reads it during ordinary composition).
            var contextMenuRow by remember { mutableStateOf<T?>(null) }
            var contextMenuCellText by remember { mutableStateOf<String?>(null) }
            val clipboard = LocalClipboardManager.current
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GridContextMenuItems(
                    items = {
                        buildList {
                            val cellText = contextMenuCellText
                            if (copyCellLabel != null && cellText != null) {
                                add(copyCellLabel to { clipboard.setText(AnnotatedString(cellText)) })
                            }
                            val target = contextMenuRow
                            if (copyRowLabel != null && target != null) {
                                add(copyRowLabel to { clipboard.setText(AnnotatedString(copyableRowsText(columns, listOf(target)))) })
                            }
                            val selectedRows = rows.filter { key(it) in selectedKeys }
                            if (copySelectedRowsLabel != null && selectable && selectedRows.isNotEmpty()) {
                                add(copySelectedRowsLabel to { clipboard.setText(AnnotatedString(copyableRowsText(columns, selectedRows))) })
                            }
                        }
                    },
                ) {
                SelectionContainer {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(rows, key = key) { row ->
                            val current = key(row) in selectedKeys
                            val dimmed = rowDimmed?.invoke(row) == true
                            val tint = rowTint?.invoke(row)
                            val rowBackground = when {
                                current -> colors.selectedRowContainerColor
                                dimmed -> colors.dimmedRowContainerColor
                                tint != null -> tint
                                else -> colors.rowContainerColor
                            }
                            val rowAlpha = if (dimmed) 0.55f else 1f

                            // rememberUpdatedState so rowTapGestures' pointerInput below can be
                            // keyed on the tap-timing flags alone (see its own doc) instead of
                            // on these callback lambdas, which are fresh instances every
                            // recomposition — keying on them would restart the gesture-
                            // detection coroutine (and risk dropping a tap mid-gesture) on
                            // every unrelated recomposition of this row.
                            val onTap = rememberUpdatedState<() -> Unit> {
                                when {
                                    openOnSingleTap -> onRowOpen(row)
                                    selectable -> onSelect(key(row))
                                }
                                onRowTap?.invoke(row, false)
                            }
                            val onDoubleTap = rememberUpdatedState<() -> Unit> {
                                onRowOpen(row)
                                onRowTap?.invoke(row, true)
                            }

                            Row(
                                // scrollContentWidth (not totalWidth) so this row's own hScroll
                                // usage agrees with the header's on how far there is to scroll —
                                // see the filter row's comment. Base line-drawing here is bounded
                                // to totalWidthPx for the same reason: every DATA column below
                                // paints its own matching background + boundary line and carries
                                // them along during a drag (see columnCell's doc); this Row only
                                // draws the order-independent bottom/selection-column lines.
                                //
                                // Dimming is baked directly into each cell's own background color
                                // (rowAlpha, applied per-cell below inside columnCell), never via
                                // a Modifier.alpha() wrapping this whole Row — see columnCell's
                                // doc on contentAlpha for why an alpha<1 layer spanning multiple
                                // independently-translating sibling cells breaks during a drag.
                                //
                                // No rect painted here at all for the row's own tint — every
                                // cell (dragged or static) already paints its own rowBackground-
                                // colored square on top of this; whatever slot no cell currently
                                // covers (the trailing margin, and — mid-drag — the gap the
                                // dragged column is vacating before a sibling's shift animation
                                // catches up) shows through to whatever is structurally behind
                                // this Row (the grid's own colors.containerColor) instead.
                                Modifier.horizontalScroll(hScroll).width(scrollContentWidth).height(ROW_HEIGHT)
                                    .drawBehind {
                                        staticRowLines(colors.lineColor, selectionColumnWidth, totalWidthPx)
                                    }
                                    .rowTapGestures(
                                        singleTapEnabled = singleTapEnabled,
                                        doubleTapEnabled = doubleTapEnabled,
                                        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
                                        onTap = onTap,
                                        onDoubleTap = onDoubleTap,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selectable) {
                                    Box(
                                        Modifier.width(SELECTION_COLUMN_WIDTH).fillMaxHeight().alpha(rowAlpha),
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
                                    // Same self-contained cell treatment as the header/filter
                                    // cells for this column (columnCell) — own background
                                    // (this ROW's own selected/dimmed/tint/normal color, so a
                                    // displaced cell in a tinted row doesn't flash a mismatched
                                    // color), own boundary line, own reorder-drag translation.
                                    // Reads the SAME Animatable the header loop drives.
                                    val shiftAnim = remember(c.id) { columnShift.shiftAnimatable(c.id) }
                                    // The dragged cell keeps ITS OWN color (rowBackground) — the
                                    // same green/gray/white it had before it was picked up —
                                    // rather than switching to a generic one, so a selected
                                    // (green) row's cell still reads as that row's cell while
                                    // being carried around. It's exempted from this row's own
                                    // dimming alpha while dragging (contentAlpha stays 1f): the
                                    // header cell it's otherwise matching is never dimmed at
                                    // all. Dimming resumes once it's dropped back into normal
                                    // (non-dragged) flow.
                                    val draggingThis = editable && dragIndex == i
                                    val cellText = c.cell.copyText(row)
                                    Box(
                                        // fillMaxHeight matters here for the border/background
                                        // painted by columnCell below: without an explicit
                                        // height, this Box only wraps its Text's own (shorter)
                                        // line height, not the full ROW_HEIGHT the Row itself
                                        // is set to, so a border drawn around it would hug the
                                        // text vertically instead of matching the full cell.
                                        Modifier.width(effective[i]).fillMaxHeight()
                                            .columnCell(
                                                i,
                                                backgroundColor = rowBackground,
                                                lineColor = colors.lineColor,
                                                zIndexValue = columnZIndex(i, dragIndex, hoverIndex),
                                                editable = editable,
                                                contentAlpha = if (draggingThis) 1f else rowAlpha,
                                                dragIndexOf = dragIndexOf, dragOffsetXOf = dragOffsetXOf, shiftValueOf = { shiftAnim.value },
                                            )
                                            // Records which row/cell the context menu wrapping
                                            // the WHOLE SelectionContainer below should build
                                            // "Copy cell"/"Copy row" items for. It has to be
                                            // recorded here, at the actual click point, and read
                                            // back at an ANCESTOR of SelectionContainer — the
                                            // platform's own context-menu-opening logic only
                                            // consults context-menu items provided by ancestors
                                            // of wherever it's rooted (SelectionContainer's own
                                            // composition point), not by arbitrary descendants
                                            // added deeper inside its lazily-composed content.
                                            .onRightClick(row, cellText) {
                                                contextMenuRow = row
                                                contextMenuCellText = cellText
                                            }
                                            .padding(horizontal = 8.dp),
                                    ) {
                                        when (val cell = c.cell) {
                                            is ExcelComposeCell.CustomCell -> cell.content(row)
                                            is ExcelComposeCell.TextCell -> Text(
                                                cell.value(row),
                                                style = MaterialTheme.typography.bodySmall,
                                                // Only overridden while this exact cell is being
                                                // dragged: contentAlpha above is forced to 1f then
                                                // (so the cell's own background stays fully opaque
                                                // instead of fading with the rest of the row),
                                                // which would otherwise also undo this row's text
                                                // dimming, since alpha affects the whole cell
                                                // uniformly. Pre-compositing the same dimmed tone
                                                // as an explicit color keeps the text gray without
                                                // depending on alpha.
                                                color = if (dimmed && draggingThis) {
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = rowAlpha).compositeOver(rowBackground)
                                                } else Color.Unspecified,
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
                }

                // Compose's own VerticalScrollbar draws a full-track thumb (not nothing) even
                // when content already fits, which reads as a spurious control on short grids
                // (e.g. a 2-row preview) — ScrollbarVisibility.Overflow (the default) guards
                // against exactly that; Hidden/Visible are unconditional either way.
                val showVertical = when (verticalScrollbar) {
                    ScrollbarVisibility.Hidden -> false
                    ScrollbarVisibility.Overflow -> listState.canScrollForward || listState.canScrollBackward
                    ScrollbarVisibility.Visible -> true
                }
                if (showVertical) {
                    GridVerticalScrollbar(
                        listState,
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 2.dp),
                    )
                }

                if (!loading && rows.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { emptyState() }
                }
            }

            val showHorizontal = when (horizontalScrollbar) {
                ScrollbarVisibility.Hidden -> false
                ScrollbarVisibility.Overflow -> hScroll.canScrollForward || hScroll.canScrollBackward
                ScrollbarVisibility.Visible -> true
            }
            if (showHorizontal) {
                GridHorizontalScrollbar(hScroll, Modifier.fillMaxWidth().padding(horizontal = 2.dp))
            }
        }

        // Single, absolutely-positioned selection outline spanning the header, the filter
        // row, and every currently visible body row for the dragged column, instead of the
        // header/filter/every visible body row cell each drawing its OWN matching border and
        // relying on them lining up by identical color/inset/position. One Box, one border,
        // computed fresh from the dragged column's current position and the list's visible
        // extent, declared LAST (after the Column above) so it paints on top of everything
        // else by plain declaration order — no zIndex needed.
        //
        // leftEdgePx is an offset WITHIN the scrollable content, not an on-screen position.
        // Every header/filter/body Row lives inside its own `.horizontalScroll(hScroll)`,
        // which shifts their painted content left by hScroll.value as the grid scrolls; this
        // overlay Box is a plain sibling of that Column, outside any scrolling container, so
        // it must subtract hScroll.value itself to land on the same on-screen pixel the
        // actual (scrolled) column cells do.
        if (editable) {
            val from = dragIndex
            if (from != null) {
                val leftEdgePx = columnLeftEdgePx(from, effective, selectionColumnWidth, density)
                val headerAndFilterHeight = HEADER_HEIGHT + if (filterRowEnabled) FILTER_ROW_HEIGHT else 0.dp
                // listState.layoutInfo changes every scroll frame — read here (composition
                // time) only because this whole block is already gated on an active drag, so
                // the cost is bounded to "dragging and scrolling at once", not every scroll.
                val visibleBodyHeightPx = run {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()
                    (last?.let { it.offset + it.size } ?: 0).coerceIn(0, info.viewportSize.height)
                }
                val totalHeight = headerAndFilterHeight + with(density) { visibleBodyHeightPx.toDp() }
                Box(
                    Modifier
                        .offset { IntOffset(x = (leftEdgePx + dragOffsetX - hScroll.value).roundToInt(), y = 0) }
                        .width(effective[from])
                        .height(totalHeight)
                        .border(1.5.dp, colors.selectedBorderColor),
                )
            }
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
 * Draw order for column [i] during a reorder drag: the grabbed column highest, a sibling
 * currently reflowing to make room for it next, everything untouched at the bottom — so
 * whichever column is actually in visual motion draws on top during the moments cells
 * overlap, instead of falling back to plain declaration order. [dragIndex]/[hoverIndex] both
 * change rarely enough (a handful of times per drag, not per pixel) that computing this at
 * composition time — not deferred to draw-phase like the translation itself — is safe.
 */
private fun columnZIndex(i: Int, dragIndex: Int?, hoverIndex: Int?): Float {
    if (dragIndex == null) return 0f
    if (i == dragIndex) return 100f
    val hover = hoverIndex ?: return 0f
    val inRange = when {
        hover > dragIndex -> i in (dragIndex + 1)..hover
        hover < dragIndex -> i in hover until dragIndex
        else -> false
    }
    return if (inRange) 1f else 0f
}

/**
 * The target a SIBLING column (not the one actually being dragged) should animate its
 * reorder shift toward, given where the dragged column ([from]) would land if dropped right
 * now ([hover]). Siblings between the dragged column's original slot and [hover] shift by
 * exactly the DRAGGED column's width, not their own — think of the dragged column as leaving
 * a hole exactly its own size behind it and opening an identically-sized hole at the target;
 * every sibling between old and new position shifts by that one hole size to close/open it,
 * regardless of how wide the sibling itself is (the standard model used by e.g. Android's
 * ItemTouchHelper reordering — shifting each sibling by ITS OWN width instead would land it
 * at the wrong final position whenever siblings' widths differ from each other).
 */
private fun siblingShiftTarget(i: Int, from: Int, hover: Int, draggedWidthPx: Float): Float = when {
    hover > from && i in (from + 1)..hover -> -draggedWidthPx
    hover < from && i in hover until from -> draggedWidthPx
    else -> 0f
}

/**
 * Maps a column-reorder drag's current position to an insertion index. [dragOffsetPx] is
 * the accumulated pointer delta since the drag on [from] started; [effective]/
 * [selectionColumnWidth] give each column's current on-screen left edge. Walks outward from
 * [from] one neighbor at a time, comparing the DRAGGED column's own leading edge (its right
 * edge while moving right, left edge while moving left) against each neighbor's center —
 * not the dragged column's center against the neighbor's, which would make a column WIDER
 * than its neighbor unable to ever fully cross it (its center alone can't travel far enough
 * while its own edge still could). The result is exactly the index [from] should land at
 * once removed from the list, matching the semantics documented on [DataGrid]'s `onReorder`.
 */
private fun resolveDropTarget(
    from: Int,
    dragOffsetPx: Float,
    effective: List<Dp>,
    selectionColumnWidth: Dp,
    density: Density,
): Int {
    val widthsPx = with(density) { effective.map { it.toPx() } }
    val leftEdgesPx = FloatArray(widthsPx.size)
    var x = with(density) { selectionColumnWidth.toPx() }
    widthsPx.forEachIndexed { i, w -> leftEdgesPx[i] = x; x += w }

    val draggedLeft = leftEdgesPx[from] + dragOffsetPx
    val draggedRight = draggedLeft + widthsPx[from]

    var target = from
    when {
        dragOffsetPx > 0f -> while (target < widthsPx.lastIndex) {
            val neighbor = target + 1
            val neighborCenter = leftEdgesPx[neighbor] + widthsPx[neighbor] / 2f
            if (draggedRight > neighborCenter) target++ else break
        }
        dragOffsetPx < 0f -> while (target > 0) {
            val neighbor = target - 1
            val neighborCenter = leftEdgesPx[neighbor] + widthsPx[neighbor] / 2f
            if (draggedLeft < neighborCenter) target-- else break
        }
    }
    return target
}

/**
 * On-screen left edge, in px, of the column at [index] (or of the space right after the
 * last column, when `index == effective.size`) — [effective]'s entries up to (not including)
 * [index] summed, plus [selectionColumnWidth]. Shared by the reorder drag's clamp, the
 * selection-overlay border, and (via `index == effective.size`) anywhere the total column
 * width in px is needed.
 *
 * Example: `columnLeftEdgePx(2, effective, selectionColumnWidth, density)` — the x
 * coordinate where column index 2 begins.
 */
private fun columnLeftEdgePx(index: Int, effective: List<Dp>, selectionColumnWidth: Dp, density: Density): Float =
    with(density) { selectionColumnWidth.toPx() + effective.take(index).sumOf { it.toPx().toDouble() }.toFloat() }

/** This column's shared shift Animatable, created on first use — see `columnShift`'s doc on [DataGrid]. */
private fun MutableMap<String, Animatable<Float, AnimationVector1D>>.shiftAnimatable(id: String): Animatable<Float, AnimationVector1D> =
    getOrPut(id) { Animatable(0f, Float.VectorConverter) }

/**
 * CSV-ish text for [rows] under [columns] — a heading line ([GridColumn.heading] for every
 * copyable column), then one comma-separated line per row, each column's
 * [ExcelComposeCell.copyText]. A column with no copyText at all (an [ExcelComposeCell.
 * CustomCell] with no `copyValue`) is left out of both the heading and every row's line, so
 * the two always line up. Backs [DataGrid]'s `copyCellLabel`/`copyRowLabel`/
 * `copySelectedRowsLabel`.
 *
 * Example: `copyableRowsText(columns, listOf(row))` — heading line + that one row's line.
 */
private fun <T> copyableRowsText(columns: List<GridColumn<T>>, rows: List<T>): String {
    val copyableColumns = columns.filter { col ->
        when (val cell = col.cell) {
            is ExcelComposeCell.TextCell -> true
            is ExcelComposeCell.CustomCell -> cell.copyValue != null
        }
    }
    val heading = copyableColumns.joinToString(", ") { it.heading }
    val lines = rows.map { row -> copyableColumns.joinToString(", ") { col -> col.cell.copyText(row).orEmpty() } }
    return (listOf(heading) + lines).joinToString("\n")
}

/**
 * "+" button pinned to the header's top-right corner (see call site for why it's a
 * fixed sibling of the scrolling header, not part of its scrollable content). A small
 * rounded background chip, deliberately without its own border — a border here would sit
 * as a separate rectangle past the last column's own boundary line, at a slightly
 * different height (this button's 24dp inside the header's 32dp) than the header's clean
 * row line, rather than reading as a continuation of it.
 */
@Composable
private fun AddColumnButton(onClick: () -> Unit, colors: ExcelGridColors, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(start = 4.dp)
            .size(24.dp)
            .background(colors.headerContainerColor, FilterCellShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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
 *
 * [onTap]/[onDoubleTap] are [State] (pass `rememberUpdatedState { ... }` at the call site),
 * not plain lambdas — the underlying `pointerInput` key list only includes the tap-timing
 * flags, which rarely change, so the long-lived gesture-detection coroutine survives
 * ordinary recompositions instead of restarting (and risking a dropped mid-gesture tap)
 * every time the caller passes a structurally-fresh lambda.
 */
private fun Modifier.rowTapGestures(
    singleTapEnabled: Boolean,
    doubleTapEnabled: Boolean,
    doubleTapTimeoutMillis: Long,
    onTap: State<() -> Unit>,
    onDoubleTap: State<() -> Unit>,
): Modifier {
    if (!singleTapEnabled && !doubleTapEnabled) return this
    return pointerInput(singleTapEnabled, doubleTapEnabled, doubleTapTimeoutMillis) {
        awaitEachGesture {
            awaitFirstDown()
            if (waitForUpOrCancellation() == null) return@awaitEachGesture // drag/scroll, not a tap

            if (!doubleTapEnabled) {
                onTap.value()
                return@awaitEachGesture
            }

            val secondDown = withTimeoutOrNull(doubleTapTimeoutMillis) { awaitFirstDown() }
            if (secondDown == null) {
                if (singleTapEnabled) onTap.value() // window passed with no second tap: a genuine single
                return@awaitEachGesture
            }
            if (waitForUpOrCancellation() != null) {
                onDoubleTap.value()
            } else if (singleTapEnabled) {
                onTap.value() // second press turned into a drag — fall back to treating the first as a single
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

/**
 * Bottom row separator + (if selectable) the line right after the selection column — the
 * ONLY grid lines drawn once, shared, by a parent Row. Neither depends on DATA column order,
 * unlike the border between two data columns, which is each column's OWN responsibility
 * (see [columnCell]) so it moves WITH the column during a reorder drag. [lineWidthPx]
 * defaults to this DrawScope's own width, but a caller whose Row is measured wider than its
 * real columns (the "+" button's reserved scroll space) passes the narrower, real width
 * explicitly so the line doesn't extend into that empty space.
 */
private fun DrawScope.staticRowLines(color: Color, selectionColumnWidth: Dp, lineWidthPx: Float = size.width) {
    val thickness = 1.dp.toPx()
    if (selectionColumnWidth > 0.dp) {
        val x = selectionColumnWidth.toPx()
        drawLine(color, Offset(x, 0f), Offset(x, size.height), thickness)
    }
    val y = size.height - thickness / 2f
    drawLine(color, Offset(0f, y), Offset(lineWidthPx, y), thickness)
}

/**
 * The full self-contained visual for ONE data-column cell — background, its own right-edge
 * and bottom-edge divider lines, and (only when [editable]) the reorder-drag translation —
 * used identically for a column's header cell, its filter cell, and every visible body row's
 * cell. Every column always paints its own background and boundary lines, unconditionally,
 * whether anything is being dragged or not: there is only ever ONE thing drawn at any given
 * screen position, with nothing left for a second, shared layer to fall out of sync with.
 * `graphicsLayer` (used for the drag translation below) only affects the draw phase — never
 * layout, hit-testing, or z-order — so a translated cell's paint is always an ADDITIONAL
 * layer on top of whatever else occupies that screen position; owning the background here
 * per-cell is what keeps that additional layer the only thing drawn there.
 *
 * [zIndexValue] should rank higher for the actively-dragged column, a bit lower for a
 * sibling currently reflowing to make room for it, and 0 for anything untouched — so
 * whichever column is actually in motion draws on top during the moment cells visually
 * overlap, instead of falling back to plain declaration order.
 *
 * [contentAlpha] (e.g. for a dimmed row) is applied HERE, per cell, inside the SAME
 * graphicsLayer that sets `translationX` — never as a separate outer `Modifier.alpha()`.
 * Two reasons: (1) `alpha < 1f` forces Compose to render its subtree into an offscreen
 * layer before compositing it, and if that subtree spanned MULTIPLE sibling cells that
 * independently translate via their own nested graphicsLayer (mid reorder-drag), overlap
 * between them that a fully-opaque row would simply paint over correctly would become
 * visible instead — scoping alpha to one cell at a time avoids ever building a shared
 * offscreen layer across overlapping siblings. (2) Even scoped to one cell, an alpha<1f
 * layer sitting OUTSIDE (wrapping) a translating one is sized and positioned to that cell's
 * OWN, ORIGINAL (untranslated) bounds — its offscreen buffer has no idea the inner layer
 * moves at all, so any part of the translated content landing outside those original bounds
 * is silently clipped there instead of painted. One graphicsLayer, alpha and translationX
 * set together, has no such seam: translation moves where the whole (already-dimmed) layer
 * paints, so nothing about it is bounds-clipped by itself.
 *
 * [dragIndexOf]/[dragOffsetXOf]/[shiftValueOf] are lambdas so the state reads they wrap
 * happen INSIDE the graphicsLayer draw-phase block, not at this function's call site —
 * reading them eagerly here would subscribe the enclosing composable to every pixel of drag
 * movement or every animation frame, recomposing instead of just redrawing one layer.
 *
 * Example: `Modifier.width(colWidth).columnCell(i, rowBackground, lineColor, zIndex,
 * editable = true, dragIndexOf = { dragIndex }, dragOffsetXOf = { dragOffsetX },
 * shiftValueOf = { shiftAnim.value })`.
 */
private fun Modifier.columnCell(
    i: Int,
    backgroundColor: Color,
    lineColor: Color,
    zIndexValue: Float,
    editable: Boolean,
    contentAlpha: Float = 1f,
    dragIndexOf: () -> Int?,
    dragOffsetXOf: () -> Float,
    shiftValueOf: () -> Float,
): Modifier = this
    // zIndex is parent-data read by the enclosing Row when it decides paint order — kept as
    // the OUTERMOST modifier here (before the graphicsLayer below) so it's never in question
    // whether a nested compositing layer (alpha<1, or the shadow's own offscreen layer while
    // dragging) affects how the parent sees it.
    .zIndex(zIndexValue)
    .graphicsLayer {
        // alpha lives HERE, alongside translationX, in this one layer — see the doc above on
        // why a separate outer Modifier.alpha() would clip a translated cell instead of
        // drawing it.
        this.alpha = contentAlpha
        if (editable) {
            val from = dragIndexOf()
            if (i == from) {
                translationX = dragOffsetXOf()
                // shadowElevation on a graphicsLayer needs an explicit shape + clip=true to
                // composite correctly with whatever's drawn after it in the chain (the
                // background below) — without both, the shadow can render with no solid
                // backing under it.
                shadowElevation = 4.dp.toPx()
                shape = RectangleShape
                clip = true
            } else {
                translationX = shiftValueOf()
            }
        }
    }
    .background(backgroundColor)
    .drawBehind {
        // Right AND bottom edge, both drawn here (after — i.e. on top of — this SAME cell's
        // own .background() above), not by a shared row-level pass: every cell paints its own
        // opaque background covering its full bounds, which would otherwise hide any grid
        // line a parent Row tried to draw underneath before its children paint over it.
        // Owning both edges here means there's nothing underneath for a cell to accidentally
        // cover.
        val thickness = 1.dp.toPx()
        drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), thickness)
        val y = size.height - thickness / 2f
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), thickness)
    }
