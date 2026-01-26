package org.jellyfin.androidtv.ui.player.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.map
import org.jellyfin.androidtv.data.service.BackgroundService
import org.jellyfin.androidtv.ui.ScreensaverLock
import org.jellyfin.androidtv.ui.player.base.PlayerSurface
import org.jellyfin.playback.core.PlaybackManager
import org.jellyfin.playback.core.model.PlayState
import org.jellyfin.playback.core.queue.queue
import org.jellyfin.playback.jellyfin.queue.createBaseItemQueueEntry
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.MediaType
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val DefaultVideoAspectRatio = 16f / 9f

@Composable
fun PhotoPlayerScreen() {
	val viewModel = koinViewModel<PhotoPlayerViewModel>()
	val item by viewModel.currentItem.collectAsState()
	val presentationActive by viewModel.presentationActive.collectAsState()

	val backgroundService = koinInject<BackgroundService>()
	LaunchedEffect(backgroundService) {
		backgroundService.clearBackgrounds()
	}

	val isVideo by remember { derivedStateOf { item?.mediaType == MediaType.VIDEO } }
	val playbackManager = koinInject<PlaybackManager>()
	val api = koinInject<ApiClient>()

	// Start video playback when a video item is current
	LaunchedEffect(item) {
		val currentItem = item ?: return@LaunchedEffect

		if (currentItem.mediaType == MediaType.VIDEO) {
			viewModel.pausePresentationForVideo()

			playbackManager.queue.clear()
			val queueEntry = createBaseItemQueueEntry(api, currentItem)
			playbackManager.queue.addSupplier(object : org.jellyfin.playback.core.queue.supplier.QueueSupplier {
				override val size: Int = 1
				override suspend fun getItem(index: Int) = if (index == 0) queueEntry else null
			})
			playbackManager.state.play()
		} else {
			playbackManager.state.stop()
		}
	}

	// Listen for video playback completion to advance to next item
	val playState by playbackManager.state.playState.collectAsState()
	LaunchedEffect(playState, isVideo, item) {
		// Only trigger completion when:
		// 1. Current item is a video
		// 2. Play state changed to STOPPED
		// 3. We had actually been playing (not just initializing)
		if (isVideo && playState == PlayState.STOPPED) {
			val queueEmpty = playbackManager.queue.entry.value == null
			if (queueEmpty) {
				viewModel.onVideoCompleted()
			}
		}
	}

	// Clean up video playback when leaving
	DisposableEffect(Unit) {
		onDispose {
			playbackManager.state.stop()
		}
	}

	// Keep screen on during video playback or photo presentation
	val videoPlaying by remember {
		playbackManager.state.playState.map { it == PlayState.PLAYING }
	}.collectAsState(false)
	ScreensaverLock(
		enabled = presentationActive || videoPlaying,
	)

	Box(
		modifier = Modifier
			.background(Color.Black)
			.fillMaxSize()
	) {
		// Always render PlayerSurface to ensure the video surface is attached and ready
		// before playback starts. This prevents the black screen issue where video audio
		// plays but video is not visible until pause/resume.
		val videoSize by playbackManager.state.videoSize.collectAsState()
		val aspectRatio = videoSize.aspectRatio.takeIf { !it.isNaN() && it > 0f } ?: DefaultVideoAspectRatio

		PlayerSurface(
			playbackManager = playbackManager,
			modifier = Modifier
				.aspectRatio(aspectRatio, videoSize.height < videoSize.width)
				.fillMaxSize()
				.align(Alignment.Center)
		)

		if (!isVideo) {
			PhotoPlayerContent(
				item = item,
			)
		}

		PhotoPlayerOverlay(
			item = item,
		)
	}
}
