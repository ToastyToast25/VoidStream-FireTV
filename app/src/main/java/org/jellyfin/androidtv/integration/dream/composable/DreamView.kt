package org.jellyfin.androidtv.integration.dream.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jellyfin.androidtv.integration.dream.model.DreamContent
import org.jellyfin.androidtv.preference.UserPreferences
import org.koin.compose.koinInject

@Composable
fun DreamView(
	content: DreamContent,
	showClock: Boolean,
) {
	val userPreferences: UserPreferences = koinInject()
	val dimmingLevel = userPreferences[UserPreferences.screensaverDimmingLevel]

	Box(
		modifier = Modifier
			.fillMaxSize()
	) {
		AnimatedContent(
			targetState = content,
			transitionSpec = {
				fadeIn(tween(durationMillis = 1_000)) togetherWith fadeOut(snap(delayMillis = 1_000))
			},
			label = "DreamContentTransition"
		) { content ->
			when (content) {
				DreamContent.Logo -> DreamContentLogo()
				is DreamContent.LibraryShowcase -> DreamContentLibraryShowcase(content)
				is DreamContent.NowPlaying -> DreamContentNowPlaying(content)
			}
		}

		// Dimming overlay
		if (dimmingLevel > 0) {
			Box(
				modifier = Modifier
					.fillMaxSize()
					.background(Color.Black.copy(alpha = dimmingLevel / 100f))
			)
		}

		// Header overlay
		DreamHeader(
			showClock = showClock,
		)
	}
}
