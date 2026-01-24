package org.jellyfin.playback.core

import kotlin.time.Duration

class PlaybackManagerOptions(
	val playerVolumeState: PlayerVolumeState,

	val defaultRewindAmount: () -> Duration,
	val defaultFastForwardAmount: () -> Duration,
	val unpauseRewindAmount: () -> Duration = { Duration.ZERO },
)
