package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.ui.composable.modifier.overscan
import org.jellyfin.androidtv.ui.shared.toolbar.ToolbarClock
import kotlin.random.Random

@Composable
fun DreamHeader(
	showClock: Boolean,
) {
	val configuration = LocalConfiguration.current
	val screenWidth = configuration.screenWidthDp
	val screenHeight = configuration.screenHeightDp
	val clockWidth = 150
	val clockHeight = 50
	
	val offsetX = remember { Animatable((screenWidth - clockWidth - 50).toFloat()) }
	val offsetY = remember { Animatable(50f) }
	
	LaunchedEffect(showClock) {
		if (showClock) {
			while (true) {
				delay(60_000) // Move every 60 seconds
				val newX = Random.nextInt(50, (screenWidth - clockWidth - 50).coerceAtLeast(51))
				val newY = Random.nextInt(50, (screenHeight - clockHeight - 50).coerceAtLeast(51))
				offsetX.animateTo(newX.toFloat(), animationSpec = tween(durationMillis = 2000))
				offsetY.animateTo(newY.toFloat(), animationSpec = tween(durationMillis = 2000))
			}
		}
	}
	
	Box(
		modifier = Modifier
			.fillMaxSize()
			.overscan(),
	) {
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
