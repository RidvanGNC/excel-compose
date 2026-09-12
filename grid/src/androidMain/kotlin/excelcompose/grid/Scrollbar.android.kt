package excelcompose.grid

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun GridVerticalScrollbar(listState: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun GridHorizontalScrollbar(scrollState: ScrollState, modifier: Modifier) = Unit
