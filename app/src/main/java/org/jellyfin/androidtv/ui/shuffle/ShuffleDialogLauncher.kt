package org.jellyfin.androidtv.ui.shuffle

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.model.AggregatedLibrary
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * Show the shuffle dialog from Java code (e.g., FullDetailsFragment)
 */
@Suppress("UNUSED_PARAMETER")
fun showShuffleDialog(
	context: android.content.Context,
	navigationRepository: NavigationRepository
) {
	val dialog = androidx.appcompat.app.AppCompatDialog(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
	dialog.setContentView(
		androidx.compose.ui.platform.ComposeView(context).apply {
			setContent {
				JellyfinTheme {
					val api = koinInject<ApiClient>()
					val shuffleManager = koinInject<ShuffleManager>()
					val userPreferences = koinInject<UserPreferences>()
					val userViewsRepository = koinInject<UserViewsRepository>()
					val multiServerRepository = koinInject<MultiServerRepository>()

					var userViews by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
					var enableMultiServer by remember { mutableStateOf(false) }
					var shuffleContentType by remember { mutableStateOf("both") }
					var aggregatedLibraries by remember { mutableStateOf<List<AggregatedLibrary>>(emptyList()) }
					var showDialog by remember { mutableStateOf(true) }

					LaunchedEffect(Unit) {
						try {
							enableMultiServer = userPreferences[UserPreferences.enableMultiServerLibraries]
							shuffleContentType = userPreferences[UserPreferences.shuffleContentType]
							val views = userViewsRepository.views.first()
							userViews = views.toList()
							if (enableMultiServer) {
								try {
									aggregatedLibraries = withContext(Dispatchers.IO) {
										multiServerRepository.getAggregatedLibraries()
									}
								} catch (e: Exception) {
									Timber.e(e, "Failed to load aggregated libraries")
								}
							}
						} catch (e: Exception) {
							Timber.e(e, "Failed to load user views")
						}
					}

					if (showDialog) {
						ShuffleOptionsDialog(
							userViews = userViews,
							aggregatedLibraries = aggregatedLibraries,
							enableMultiServer = enableMultiServer,
							shuffleContentType = shuffleContentType,
							api = api,
							onDismiss = { 
								showDialog = false
								dialog.dismiss()
							},
							onShuffle = { libraryId, serverId, genreName, contentType, libraryCollectionType ->
							// Use a non-compose scope so the shuffle survives dialog dismissal
							kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
								shuffleManager.libraryShuffle(
									context = context,
									libraryId = libraryId,
									serverId = serverId,
									genreName = genreName,
									contentType = contentType,
									libraryCollectionType = libraryCollectionType
								)
							}
							showDialog = false
							dialog.dismiss()
							}
						)
					}
				}
			}
		}
	)
	dialog.show()
}
