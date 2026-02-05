package org.jellyfin.androidtv.ui.shuffle

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.util.sdk.ApiClientFactory
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy
import timber.log.Timber
import java.util.UUID
import kotlin.random.Random

/**
 * Centralized manager for shuffle functionality.
 * Handles loading state, debouncing, user feedback, and client-side random selection.
 */
class ShuffleManager(
	private val api: ApiClient,
	private val apiClientFactory: ApiClientFactory,
	private val userPreferences: UserPreferences,
	private val navigationRepository: NavigationRepository,
) {
	private val _isShuffling = MutableStateFlow(false)
	val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

	private val shuffleMutex = Mutex()

	/**
	 * Quick shuffle - uses user's content type preference
	 */
	suspend fun quickShuffle(context: Context) {
		val contentType = userPreferences[UserPreferences.shuffleContentType]
		shuffle(
			context = context,
			libraryId = null,
			serverId = null,
			genreName = null,
			contentType = contentType,
			libraryCollectionType = null
		)
	}

	/**
	 * Shuffle within a specific genre
	 */
	suspend fun genreShuffle(
		context: Context,
		genreName: String,
		libraryId: UUID?,
		serverId: UUID?
	) {
		val contentType = userPreferences[UserPreferences.shuffleContentType]
		shuffle(
			context = context,
			libraryId = libraryId,
			serverId = serverId,
			genreName = genreName,
			contentType = contentType,
			libraryCollectionType = null
		)
	}

	/**
	 * Shuffle within a specific library (from shuffle dialog)
	 */
	suspend fun libraryShuffle(
		context: Context,
		libraryId: UUID?,
		serverId: UUID?,
		genreName: String?,
		contentType: String?,
		libraryCollectionType: CollectionType?
	) {
		shuffle(
			context = context,
			libraryId = libraryId,
			serverId = serverId,
			genreName = genreName,
			contentType = contentType ?: userPreferences[UserPreferences.shuffleContentType],
			libraryCollectionType = libraryCollectionType
		)
	}

	/**
	 * Core shuffle logic with debouncing, loading state, and client-side random selection
	 */
	suspend fun shuffle(
		context: Context,
		libraryId: UUID?,
		serverId: UUID?,
		genreName: String?,
		contentType: String,
		libraryCollectionType: CollectionType?
	) {
		// Debounce - don't allow multiple simultaneous shuffles
		if (!shuffleMutex.tryLock()) {
			Timber.d("Shuffle already in progress, ignoring request")
			return
		}

		try {
			_isShuffling.value = true

			val includeTypes = determineIncludeTypes(contentType, libraryCollectionType)
			val targetApi = if (serverId != null) {
				apiClientFactory.getApiClientForServer(serverId) ?: api
			} else {
				api
			}

			val randomItem = withContext(Dispatchers.IO) {
				fetchRandomItem(targetApi, libraryId, genreName, includeTypes)
			}

			if (randomItem != null) {
				Timber.i("Shuffle found: ${randomItem.name} (${randomItem.type})")
				navigationRepository.navigate(Destinations.itemDetails(randomItem.id, serverId))
			} else {
				Timber.w("No items found for shuffle")
				withContext(Dispatchers.Main) {
					Toast.makeText(
						context,
						context.getString(R.string.shuffle_no_items_found),
						Toast.LENGTH_SHORT
					).show()
				}
			}
		} catch (e: Exception) {
			Timber.e(e, "Shuffle failed")
			withContext(Dispatchers.Main) {
				Toast.makeText(
					context,
					context.getString(R.string.shuffle_error),
					Toast.LENGTH_SHORT
				).show()
			}
		} finally {
			_isShuffling.value = false
			shuffleMutex.unlock()
		}
	}

	private fun determineIncludeTypes(
		contentType: String,
		libraryCollectionType: CollectionType?
	): Set<BaseItemKind> {
		return when {
			libraryCollectionType == CollectionType.MOVIES -> setOf(BaseItemKind.MOVIE)
			libraryCollectionType == CollectionType.TVSHOWS -> setOf(BaseItemKind.SERIES)
			contentType == "movies" -> setOf(BaseItemKind.MOVIE)
			contentType == "tv" -> setOf(BaseItemKind.SERIES)
			else -> setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
		}
	}

	/**
	 * Fetch a random item using server-side RANDOM sort (single API call).
	 * Falls back to client-side random selection if server-side fails.
	 * Retries if server returns excluded item types (server bug workaround).
	 */
	private suspend fun fetchRandomItem(
		targetApi: ApiClient,
		libraryId: UUID?,
		genreName: String?,
		includeTypes: Set<BaseItemKind>
	): BaseItemDto? {
		val maxRetries = 5
		
		// Primary: Use server-side RANDOM sort with retries for excluded item types
		for (attempt in 1..maxRetries) {
			try {
				Timber.d("Shuffle: Using server-side RANDOM sort (attempt $attempt)")
				val response = targetApi.itemsApi.getItems(
					parentId = libraryId,
					genres = genreName?.let { setOf(it) },
					includeItemTypes = includeTypes,
					excludeItemTypes = setOf(BaseItemKind.BOX_SET),
					recursive = true,
					sortBy = setOf(ItemSortBy.RANDOM),
					limit = 1,
				)

				val item = response.content.items?.firstOrNull()
				if (item == null) {
					val totalCount = response.content.totalRecordCount ?: 0
					Timber.d("Shuffle: Server returned no items, totalRecordCount = $totalCount")
					return null
				}
				
				// Server-side excludeItemTypes with RANDOM sort is buggy - filter client-side
				if (item.type == BaseItemKind.BOX_SET) {
					Timber.w("Shuffle: Server returned BoxSet despite exclude filter, retrying...")
					continue
				}
				
				return item
			} catch (e: Exception) {
				Timber.w(e, "Server-side RANDOM failed on attempt $attempt")
				break // Fall through to client-side fallback
			}
		}
		
		Timber.w("Server-side random exhausted retries or failed, falling back to client-side random")
		
		// Fallback: Client-side random (two API calls)
		try {
			val countResponse = targetApi.itemsApi.getItems(
				parentId = libraryId,
				genres = genreName?.let { setOf(it) },
				includeItemTypes = includeTypes,
				excludeItemTypes = setOf(BaseItemKind.BOX_SET),
				recursive = true,
				limit = 0,
			)

			val totalCount = countResponse.content.totalRecordCount ?: 0
			if (totalCount == 0) return null

			val randomOffset = Random.nextInt(totalCount)
			Timber.d("Shuffle: Client-side picking offset $randomOffset of $totalCount")

			val itemResponse = targetApi.itemsApi.getItems(
				parentId = libraryId,
				genres = genreName?.let { setOf(it) },
				includeItemTypes = includeTypes,
				excludeItemTypes = setOf(BaseItemKind.BOX_SET),
				recursive = true,
				startIndex = randomOffset,
				limit = 1,
				sortBy = setOf(ItemSortBy.SORT_NAME),
			)

			return itemResponse.content.items?.firstOrNull()
		} catch (fallbackException: Exception) {
			Timber.e(fallbackException, "Both shuffle methods failed")
			return null
		}
	}
}
