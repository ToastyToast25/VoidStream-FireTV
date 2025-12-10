package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import kotlin.random.Random

@Composable
fun DreamHeader(
	showClock: Boolean,
) {
	val density = LocalDensity.current
	val clockWidthDp = 150.dp
	val clockHeightDp = 50.dp
	
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.overscan(),
	) {
		// Get actual pixel dimensions
		val screenWidth = with(density) { maxWidth.toPx() }.toInt()
		val screenHeight = with(density) { maxHeight.toPx() }.toInt()
		val clockWidth = with(density) { clockWidthDp.toPx() }.toInt()
		val clockHeight = with(density) { clockHeightDp.toPx() }.toInt()
		val margin = 20f
		
		val offsetX = remember { Animatable(((screenWidth - clockWidth) / 2).toFloat()) }
		val offsetY = remember { Animatable(((screenHeight - clockHeight) / 2).toFloat()) }
		
		// DVD-style bouncing animation
		LaunchedEffect(showClock) {
			if (showClock) {
				var velocityX = if (Random.nextBoolean()) 0.5f else -0.5f
				var velocityY = if (Random.nextBoolean()) 0.5f else -0.5f
				
				while (true) {
					val minX = margin
					val minY = margin
					val maxX = (screenWidth - clockWidth - margin)
					val maxY = (screenHeight - clockHeight - margin)
					
					var newX = offsetX.value + velocityX
					var newY = offsetY.value + velocityY
					
					// Bounce off edges
					if (newX <= minX) {
						newX = minX
						velocityX = -velocityX
					} else if (newX >= maxX) {
						newX = maxX
						velocityX = -velocityX
					}
					
					if (newY <= minY) {
						newY = minY
						velocityY = -velocityY
					} else if (newY >= maxY) {
						newY = maxY
						velocityY = -velocityY
					}
					
					offsetX.snapTo(newX)
					offsetY.snapTo(newY)
					
					delay(16) // ~60 FPS
				}
			}
		}
		
		// Clock
		AnimatedVisibility(
			visible = showClock,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) },
		) {
			ToolbarClock()
		}
	}
}
