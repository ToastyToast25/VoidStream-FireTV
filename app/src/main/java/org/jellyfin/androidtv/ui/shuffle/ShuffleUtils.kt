package org.jellyfin.androidtv.ui.shuffle

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.koin.core.context.GlobalContext
import java.util.UUID

/**
 * Execute a quick shuffle from Java code.
 * @deprecated Use ShuffleManager.quickShuffle() for new code
 */
@Suppress("UNUSED_PARAMETER")
fun executeQuickShuffle(
	context: Context,
	userPreferences: UserPreferences,
	navigationRepository: NavigationRepository
) {
	val shuffleManager = GlobalContext.get().get<ShuffleManager>()
	CoroutineScope(Dispatchers.Main).launch {
		shuffleManager.quickShuffle(context)
	}
}

/**
 * Execute shuffle from a genre folder.
 */
@Suppress("UNUSED_PARAMETER")
fun executeGenreShuffle(
	context: Context,
	genreName: String?,
	libraryId: UUID?,
	serverId: UUID?,
	userPreferences: UserPreferences,
	navigationRepository: NavigationRepository
) {
	val shuffleManager = GlobalContext.get().get<ShuffleManager>()
	CoroutineScope(Dispatchers.Main).launch {
		if (genreName.isNullOrBlank()) {
			shuffleManager.quickShuffle(context)
		} else {
			shuffleManager.genreShuffle(
				context = context,
				genreName = genreName,
				libraryId = libraryId,
				serverId = serverId
			)
		}
	}
}
