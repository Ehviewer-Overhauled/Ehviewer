package my.nanihadesuka.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import kotlinx.coroutines.launch
import kotlin.math.floor

/**
 * Scrollbar for LazyColumn
 * Use this variation if you want to place the scrollbar independently of the LazyColumn position
 *
 * @param rightSide true -> right,  false -> left
 * @param thickness Thickness of the scrollbar thumb
 * @param padding Padding of the scrollbar
 * @param thumbMinHeight Thumb minimum height proportional to total scrollbar's height (eg: 0.1 -> 10% of total)
 */
@Composable
fun LazyColumnScrollbar(
    listState: LazyListState,
    rightSide: Boolean = true,
    thickness: Dp = 6.dp,
    padding: Dp = 8.dp,
    thumbMinHeight: Float = 0.1f,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    thumbSelectedColor: Color = MaterialTheme.colorScheme.primary,
    thumbShape: Shape = CircleShape,
    selectionMode: ScrollbarSelectableMode = ScrollbarSelectableMode.Thumb,
    indicatorContent: (@Composable (index: Int, isThumbSelected: Boolean) -> Unit)? = null,
) {
    val firstVisibleItemIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val coroutineScope = rememberCoroutineScope()

    var isSelected by remember { mutableStateOf(false) }

    var dragOffset by remember { mutableFloatStateOf(0f) }

    val reverseLayout by remember { derivedStateOf { listState.layoutInfo.reverseLayout } }

    val realFirstVisibleItem by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.firstOrNull {
                it.index == listState.firstVisibleItemIndex
            }
        }
    }

    val isStickyHeaderInAction by remember {
        derivedStateOf {
            val realIndex = realFirstVisibleItem?.index ?: return@derivedStateOf false
            val firstVisibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: return@derivedStateOf false
            realIndex != firstVisibleIndex
        }
    }

    fun LazyListItemInfo.fractionHiddenTop() =
        if (size == 0) 0f else -offset.toFloat() / size.toFloat()

    fun LazyListItemInfo.fractionVisibleBottom(viewportEndOffset: Int) =
        if (size == 0) 0f else (viewportEndOffset - offset).toFloat() / size.toFloat()

    val normalizedThumbSizeReal by remember {
        derivedStateOf {
            listState.layoutInfo.let {
                if (it.totalItemsCount == 0) {
                    return@let 0f
                }

                val firstItem = realFirstVisibleItem ?: return@let 0f
                val firstPartial = firstItem.fractionHiddenTop()
                val lastPartial =
                    1f - it.visibleItemsInfo.last().fractionVisibleBottom(it.viewportEndOffset)

                val realSize = it.visibleItemsInfo.size - if (isStickyHeaderInAction) 1 else 0
                val realVisibleSize = realSize.toFloat() - firstPartial - lastPartial
                realVisibleSize / it.totalItemsCount.toFloat()
            }
        }
    }

    val normalizedThumbSize by remember {
        derivedStateOf {
            normalizedThumbSizeReal.coerceAtLeast(thumbMinHeight)
        }
    }

    fun offsetCorrection(top: Float): Float {
        val topRealMax = (1f - normalizedThumbSizeReal).coerceIn(0f, 1f)
        if (normalizedThumbSizeReal >= thumbMinHeight) {
            return when {
                reverseLayout -> topRealMax - top
                else -> top
            }
        }

        val topMax = 1f - thumbMinHeight
        return when {
            reverseLayout -> (topRealMax - top) * topMax / topRealMax
            else -> top * topMax / topRealMax
        }
    }

    fun offsetCorrectionInverse(top: Float): Float {
        if (normalizedThumbSizeReal >= thumbMinHeight) {
            return top
        }
        val topRealMax = 1f - normalizedThumbSizeReal
        val topMax = 1f - thumbMinHeight
        return top * topRealMax / topMax
    }

    val normalizedOffsetPosition by remember {
        derivedStateOf {
            listState.layoutInfo.let {
                if (it.totalItemsCount == 0 || it.visibleItemsInfo.isEmpty()) {
                    return@let 0f
                }

                val firstItem = realFirstVisibleItem ?: return@let 0f
                val top = firstItem
                    .run { index.toFloat() + fractionHiddenTop() } / it.totalItemsCount.toFloat()
                offsetCorrection(top)
            }
        }
    }

    fun setDragOffset(value: Float) {
        val maxValue = (1f - normalizedThumbSize).coerceAtLeast(0f)
        dragOffset = value.coerceIn(0f, maxValue)
    }

    fun setScrollOffset(newOffset: Float) {
        setDragOffset(newOffset)
        val totalItemsCount = listState.layoutInfo.totalItemsCount.toFloat()
        val exactIndex = offsetCorrectionInverse(totalItemsCount * dragOffset)
        val index: Int = floor(exactIndex).toInt()
        val remainder: Float = exactIndex - floor(exactIndex)

        coroutineScope.launch {
            listState.scrollToItem(index = index, scrollOffset = 0)
            val offset = realFirstVisibleItem
                ?.size
                ?.let { it.toFloat() * remainder }
                ?.toInt() ?: 0
            listState.scrollToItem(index = index, scrollOffset = offset)
        }
    }

    val isInAction = listState.isScrollInProgress || isSelected

    val alpha by animateFloatAsState(
        targetValue = if (isInAction) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isInAction) 75 else 500,
            delayMillis = if (isInAction) 0 else 500,
        ),
        label = "alpha",
    )

    val displacement by animateFloatAsState(
        targetValue = if (isInAction) 0f else 14f,
        animationSpec = tween(
            durationMillis = if (isInAction) 75 else 500,
            delayMillis = if (isInAction) 0 else 500,
        ),
        label = "displacement",
    )

    BoxWithConstraints(
        Modifier
            .alpha(alpha)
            .fillMaxWidth(),
    ) {
        if (indicatorContent != null) {
            BoxWithConstraints(
                Modifier
                    .align(if (rightSide) Alignment.TopEnd else Alignment.TopStart)
                    .fillMaxHeight()
                    .graphicsLayer(
                        translationX = with(LocalDensity.current) { (if (rightSide) displacement.dp else -displacement.dp).toPx() },
                        translationY = constraints.maxHeight.toFloat() * normalizedOffsetPosition,
                    ),
            ) {
                ConstraintLayout(
                    Modifier.align(Alignment.TopEnd),
                ) {
                    val (box, content) = createRefs()
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(normalizedThumbSize)
                            .padding(
                                start = if (rightSide) 0.dp else padding,
                                end = if (!rightSide) 0.dp else padding,
                            )
                            .width(thickness)
                            .constrainAs(box) {
                                if (rightSide) {
                                    end.linkTo(parent.end)
                                } else {
                                    start.linkTo(parent.start)
                                }
                            },
                    )

                    Box(
                        modifier = Modifier
                            .constrainAs(content) {
                                top.linkTo(box.top)
                                bottom.linkTo(box.bottom)
                                if (rightSide) {
                                    end.linkTo(box.start)
                                } else {
                                    start.linkTo(box.end)
                                }
                            },
                    ) {
                        indicatorContent(
                            firstVisibleItemIndex.value,
                            isSelected,
                        )
                    }
                }
            }
        }

        BoxWithConstraints(
            Modifier
                .align(if (rightSide) Alignment.TopEnd else Alignment.TopStart)
                .fillMaxHeight()
                .draggable(
                    state = rememberDraggableState { delta ->
                        val displace = if (reverseLayout) -delta else delta // side effect ?
                        if (isSelected) {
                            setScrollOffset(dragOffset + displace / constraints.maxHeight.toFloat())
                        }
                    },
                    orientation = Orientation.Vertical,
                    enabled = selectionMode != ScrollbarSelectableMode.Disabled,
                    startDragImmediately = true,
                    onDragStarted = onDragStarted@{ offset ->
                        val maxHeight = constraints.maxHeight.toFloat()
                        if (maxHeight <= 0f) return@onDragStarted
                        val newOffset = when {
                            reverseLayout -> (maxHeight - offset.y) / maxHeight
                            else -> offset.y / maxHeight
                        }
                        val currentOffset = when {
                            reverseLayout -> 1f - normalizedOffsetPosition - normalizedThumbSize
                            else -> normalizedOffsetPosition
                        }
                        when (selectionMode) {
                            ScrollbarSelectableMode.Full -> {
                                if (newOffset in currentOffset..(currentOffset + normalizedThumbSize)) {
                                    setDragOffset(currentOffset)
                                } else {
                                    setScrollOffset(newOffset)
                                }
                                isSelected = true
                            }
                            ScrollbarSelectableMode.Thumb -> {
                                if (newOffset in currentOffset..(currentOffset + normalizedThumbSize)) {
                                    setDragOffset(currentOffset)
                                    isSelected = true
                                }
                            }
                            ScrollbarSelectableMode.Disabled -> Unit
                        }
                    },
                    onDragStopped = {
                        isSelected = false
                    },
                )
                .graphicsLayer(translationX = with(LocalDensity.current) { (if (rightSide) displacement.dp else -displacement.dp).toPx() }),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer(translationY = constraints.maxHeight.toFloat() * normalizedOffsetPosition)
                    .padding(horizontal = padding)
                    .width(thickness)
                    .clip(thumbShape)
                    .background(if (isSelected) thumbSelectedColor else thumbColor)
                    .fillMaxHeight(normalizedThumbSize),
            )
        }
    }
}
