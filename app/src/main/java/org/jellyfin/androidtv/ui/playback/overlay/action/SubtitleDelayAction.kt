package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class SubtitleDelayAction(
	private val context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	private val playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	
	// Delay options in milliseconds
	private val delayOptions = listOf(
		-1000L to "-1.0s",
		-750L to "-750ms",
		-500L to "-500ms",
		-250L to "-250ms",
		-100L to "-100ms",
		0L to "No Delay",
		100L to "+100ms",
		250L to "+250ms",
		500L to "+500ms",
		750L to "+750ms",
		1000L to "+1.0s",
		1500L to "+1.5s",
		2000L to "+2.0s"
	)
	
	private var currentDelayMs: Long = 0L
	private var popup: PopupMenu? = null
	
	init {
		initializeWithIcon(R.drawable.ic_subtitle_sync)
	}
	
	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = populateMenu(context, view)
		
		popup?.setOnDismissListener {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			popup = null
		}
		
		popup?.setOnMenuItemClickListener { menuItem ->
			val selectedDelay = delayOptions[menuItem.itemId]
			currentDelayMs = selectedDelay.first
			applySubtitleDelay(currentDelayMs)
			
			// Show toast feedback
			val message = if (currentDelayMs == 0L) {
				"Subtitle delay reset"
			} else {
				"Subtitle delay: ${selectedDelay.second}"
			}
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
			true
		}
		
		popup?.show()
	}
	
	private fun populateMenu(
		context: Context,
		view: View
	) = PopupMenu(context, view, Gravity.END).apply {
		delayOptions.forEachIndexed { index, (delay, label) ->
			menu.add(0, index, index, label)
		}
		
		menu.setGroupCheckable(0, true, true)
		
		// Find and check the current delay option
		val currentIndex = delayOptions.indexOfFirst { it.first == currentDelayMs }
		if (currentIndex >= 0) {
			menu.getItem(currentIndex).isChecked = true
		}
	}
	
	private fun applySubtitleDelay(delayMs: Long) {
		try {
			val videoManager = playbackController.getVideoManager() ?: return
			videoManager.setSubtitleDelay(delayMs)
			currentDelayMs = delayMs
		} catch (e: Exception) {
			Toast.makeText(context, "Failed to apply subtitle delay", Toast.LENGTH_SHORT).show()
		}
	}
	
	fun dismissPopup() {
		popup?.dismiss()
	}
	
	fun getCurrentDelay(): Long = currentDelayMs
}
