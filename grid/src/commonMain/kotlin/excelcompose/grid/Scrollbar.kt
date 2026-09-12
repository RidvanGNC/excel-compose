package excelcompose.grid

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Visible, hoverable scrollbar on desktop; no-op on Android (touch scrolling is enough). */
@Composable
expect fun GridVerticalScrollbar(listState: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun GridHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier = Modifier)
