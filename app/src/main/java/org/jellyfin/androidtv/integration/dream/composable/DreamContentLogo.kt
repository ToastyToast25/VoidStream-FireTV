package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jellyfin.androidtv.R
import kotlin.random.Random

@Composable
fun DreamContentLogo() {
	val configuration = LocalConfiguration.current
	val screenWidth = configuration.screenWidthDp
	val screenHeight = configuration.screenHeightDp
	val logoWidth = 400
	val logoHeight = 200
	
	val offsetX = remember { Animatable(((screenWidth - logoWidth) / 2).toFloat()) }
	val offsetY = remember { Animatable(((screenHeight - logoHeight) / 2).toFloat()) }
	
	LaunchedEffect(Unit) {
		while (true) {
			delay(60_000) // Move every 60 seconds
			val newX = Random.nextInt(0, (screenWidth - logoWidth).coerceAtLeast(1))
			val newY = Random.nextInt(0, (screenHeight - logoHeight).coerceAtLeast(1))
			offsetX.animateTo(newX.toFloat(), animationSpec = tween(durationMillis = 2000))
			offsetY.animateTo(newY.toFloat(), animationSpec = tween(durationMillis = 2000))
		}
	}
	
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black),
	) {
		Image(
			painter = painterResource(R.drawable.app_logo),
			contentDescription = stringResource(R.string.app_name),
			modifier = Modifier
				.offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
				.width(logoWidth.dp)
		)
	}
}
