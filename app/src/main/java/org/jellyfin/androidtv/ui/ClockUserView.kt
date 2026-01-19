package org.jellyfin.androidtv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import org.jellyfin.androidtv.databinding.ClockUserBugBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ClockUserView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0,
	defStyleRes: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes), KoinComponent {
	private val binding: ClockUserBugBinding = ClockUserBugBinding.inflate(LayoutInflater.from(context), this, true)
	private val userPreferences by inject<UserPreferences>()
	private val navigationRepository by inject<NavigationRepository>()

	var isVideoPlayer = false
		set(value) {
			field = value
			updateClockVisibility()
		}

	val homeButton get() = binding.home
	val shuffleButton get() = binding.shuffle

	private var onShuffleClickListener: Runnable? = null
	private var onShuffleLongClickListener: Runnable? = null

	init {
		updateClockVisibility()

		binding.home.setOnClickListener {
			navigationRepository.reset(Destinations.home, clearHistory = true)
		}

		binding.shuffle.setOnClickListener {
			onShuffleClickListener?.run()
		}

		binding.shuffle.setOnLongClickListener {
			onShuffleLongClickListener?.run()
			true
		}
	}

	fun setShuffleVisible(visible: Boolean) {
		binding.shuffle.visibility = if (visible) View.VISIBLE else View.GONE
	}

	fun setOnShuffleClickListener(listener: Runnable?) {
		onShuffleClickListener = listener
	}

	fun setOnShuffleLongClickListener(listener: Runnable?) {
		onShuffleLongClickListener = listener
	}

	private fun updateClockVisibility() {
		val showClock = userPreferences[UserPreferences.clockBehavior]

		binding.clock.isVisible = when (showClock) {
			ClockBehavior.ALWAYS -> true
			ClockBehavior.NEVER -> false
			ClockBehavior.IN_VIDEO -> isVideoPlayer
			ClockBehavior.IN_MENUS -> !isVideoPlayer
		}

		binding.home.isVisible = !isVideoPlayer
		if (isVideoPlayer) {
			binding.shuffle.isVisible = false
		}
	}
}
