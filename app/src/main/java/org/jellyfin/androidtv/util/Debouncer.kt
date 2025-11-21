package org.jellyfin.androidtv.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * Utility class to debounce rapid function calls.
 * Only executes the action after a specified delay has passed without new calls.
 */
class Debouncer(
	private val delay: Duration,
	private val scope: CoroutineScope,
) {
	private var debounceJob: Job? = null

	/**
	 * Schedules [action] to run after [delay]. If called again before the delay
	 * expires, the previous action is cancelled and a new one is scheduled.
	 */
	fun debounce(action: suspend () -> Unit) {
		debounceJob?.cancel()
		debounceJob = scope.launch {
			delay(delay)
			action()
		}
	}

	/**
	 * Cancels any pending debounced action.
	 */
	fun cancel() {
		debounceJob?.cancel()
		debounceJob = null
	}
}
