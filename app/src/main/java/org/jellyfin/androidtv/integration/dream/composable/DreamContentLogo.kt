package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import kotlin.random.Random

@Composable
fun DreamContentLogo() {
	val density = LocalDensity.current
	val logoWidthDp = 400.dp
	val logoHeightDp = 200.dp
	
	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black),
	) {
		// Get actual pixel dimensions
		val screenWidth = with(density) { maxWidth.toPx() }.toInt()
		val screenHeight = with(density) { maxHeight.toPx() }.toInt()
		val logoWidth = with(density) { logoWidthDp.toPx() }.toInt()
		val logoHeight = with(density) { logoHeightDp.toPx() }.toInt()
		val margin = 20f
		
		val offsetX = remember { Animatable(((screenWidth - logoWidth) / 2).toFloat()) }
		val offsetY = remember { Animatable(((screenHeight - logoHeight) / 2).toFloat()) }
		
		// DVD-style bouncing animation
		LaunchedEffect(Unit) {
			var velocityX = if (Random.nextBoolean()) 0.5f else -0.5f
			var velocityY = if (Random.nextBoolean()) 0.5f else -0.5f
			
			while (true) {
				val minX = margin
				val minY = margin
				val maxX = (screenWidth - logoWidth - margin)
				val maxY = (screenHeight - logoHeight - margin)
				
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
		
		Image(
			painter = painterResource(R.drawable.app_logo),
			contentDescription = stringResource(R.string.app_name),
			modifier = Modifier
				.offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
				.width(logoWidthDp)
		)
	}
}
