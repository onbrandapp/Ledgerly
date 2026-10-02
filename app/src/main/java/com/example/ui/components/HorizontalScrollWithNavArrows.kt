package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Circular navigation arrow button styled with crisp elevated surface and sleek chevron icons
 * placed side-by-side at the far right of the screen.
 */
@Composable
private fun NavArrowCircleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (enabled) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    }

    val borderColor = if (enabled) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
    }

    val elevation = if (enabled) 2.dp else 0.dp

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .size(34.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Navigation arrows displayed at the far right of the screen when horizontal items exit off the screen.
 */
@Composable
fun HorizontalScrollNavArrows(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    scrollStep: Float = 240f,
    testTagPrefix: String = "horizontal_nav",
    label: (@Composable () -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollBackward by remember { derivedStateOf { scrollState.value > 0 } }
    val canScrollForward by remember { derivedStateOf { scrollState.value < scrollState.maxValue } }
    val hasOffscreenItems by remember { derivedStateOf { scrollState.maxValue > 0 } }

    AnimatedVisibility(
        visible = hasOffscreenItems || label != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (label != null) Arrangement.SpaceBetween else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    label()
                }
            }

            AnimatedVisibility(
                visible = hasOffscreenItems,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavArrowCircleButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (scrollState.value - scrollStep).toInt().coerceAtLeast(0)
                                scrollState.animateScrollTo(target)
                            }
                        },
                        enabled = canScrollBackward,
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Scroll Left",
                        testTag = "${testTagPrefix}_left"
                    )

                    NavArrowCircleButton(
                        onClick = {
                            coroutineScope.launch {
                                val target = (scrollState.value + scrollStep).toInt().coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(target)
                            }
                        },
                        enabled = canScrollForward,
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Scroll Right",
                        testTag = "${testTagPrefix}_right"
                    )
                }
            }
        }
    }
}

/**
 * Container wrapping a horizontal scroll Row with left and right navigation arrows positioned at the far right.
 */
@Composable
fun HorizontalScrollWithNavArrows(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    testTagPrefix: String = "horizontal_nav",
    label: (@Composable () -> Unit)? = null,
    arrowsOnTop: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier) {
        if (arrowsOnTop || label != null) {
            HorizontalScrollNavArrows(
                scrollState = scrollState,
                testTagPrefix = testTagPrefix,
                label = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )

        if (!arrowsOnTop && label == null) {
            HorizontalScrollNavArrows(
                scrollState = scrollState,
                testTagPrefix = testTagPrefix,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

/**
 * Navigation arrows displayed at the far right of the screen for LazyRow.
 */
@Composable
fun LazyRowNavArrows(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    scrollStep: Float = 300f,
    testTagPrefix: String = "lazy_row_nav",
    label: (@Composable () -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val canScrollBackward by remember { derivedStateOf { lazyListState.canScrollBackward } }
    val canScrollForward by remember { derivedStateOf { lazyListState.canScrollForward } }
    val hasOffscreenItems by remember { derivedStateOf { lazyListState.canScrollBackward || lazyListState.canScrollForward } }

    AnimatedVisibility(
        visible = hasOffscreenItems || label != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (label != null) Arrangement.SpaceBetween else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    label()
                }
            }

            AnimatedVisibility(
                visible = hasOffscreenItems,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavArrowCircleButton(
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.animateScrollBy(-scrollStep)
                            }
                        },
                        enabled = canScrollBackward,
                        icon = Icons.Default.ChevronLeft,
                        contentDescription = "Scroll Left",
                        testTag = "${testTagPrefix}_left"
                    )

                    NavArrowCircleButton(
                        onClick = {
                            coroutineScope.launch {
                                lazyListState.animateScrollBy(scrollStep)
                            }
                        },
                        enabled = canScrollForward,
                        icon = Icons.Default.ChevronRight,
                        contentDescription = "Scroll Right",
                        testTag = "${testTagPrefix}_right"
                    )
                }
            }
        }
    }
}

/**
 * Container wrapping a LazyRow with left and right navigation arrows at the far right of the screen.
 */
@Composable
fun LazyRowWithNavArrows(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    testTagPrefix: String = "lazy_row_nav",
    label: (@Composable () -> Unit)? = null,
    arrowsOnTop: Boolean = false,
    content: LazyListScope.() -> Unit
) {
    Column(modifier = modifier) {
        if (arrowsOnTop || label != null) {
            LazyRowNavArrows(
                lazyListState = lazyListState,
                testTagPrefix = testTagPrefix,
                label = label,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        LazyRow(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            contentPadding = contentPadding,
            content = content
        )

        if (!arrowsOnTop && label == null) {
            LazyRowNavArrows(
                lazyListState = lazyListState,
                testTagPrefix = testTagPrefix,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}
