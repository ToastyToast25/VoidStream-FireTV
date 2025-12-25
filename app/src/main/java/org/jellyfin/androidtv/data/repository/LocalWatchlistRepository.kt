package org.jellyfin.androidtv.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.util.UUID

/**
 * A local watchlist item entry stored in SharedPreferences.
 * We store the item ID, server ID, and the timestamp when it was added.
 */
@Serializable
data class WatchlistEntry(
	val itemId: String,
	val serverId: String,
	val addedAt: Long = System.currentTimeMillis()
)

/**
 * Repository for managing a local app-only watchlist.
 * Items are stored locally in SharedPreferences and not synced to the server.
 */
class LocalWatchlistRepository(
	context: Context
) {
	companion object {
		private const val PREFS_NAME = "local_watchlist"
		private const val KEY_WATCHLIST = "watchlist_items"
	}

	private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
	private val json = Json { ignoreUnknownKeys = true }

	private val _watchlistFlow = MutableStateFlow<List<WatchlistEntry>>(emptyList())
	val watchlistFlow: Flow<List<WatchlistEntry>> = _watchlistFlow.asStateFlow()

	init {
		// Load initial watchlist
		_watchlistFlow.value = getWatchlistEntries()
	}

	private fun getWatchlistEntries(): List<WatchlistEntry> {
		val jsonString = prefs.getString(KEY_WATCHLIST, null) ?: return emptyList()
		return try {
			json.decodeFromString(jsonString)
		} catch (e: Exception) {
			Timber.e(e, "Failed to parse watchlist from storage")
			emptyList()
		}
	}

	private fun saveWatchlistEntries(entries: List<WatchlistEntry>) {
		val jsonString = json.encodeToString(entries)
		prefs.edit().putString(KEY_WATCHLIST, jsonString).apply()
		_watchlistFlow.value = entries
	}

	/**
	 * Add an item to the watchlist.
	 */
	fun addToWatchlist(itemId: UUID, serverId: UUID): Boolean {
		val entries = getWatchlistEntries().toMutableList()
		val itemIdStr = itemId.toString()
		val serverIdStr = serverId.toString()

		if (entries.any { it.itemId == itemIdStr && it.serverId == serverIdStr }) {
			Timber.d("Item $itemId already in watchlist")
			return false
		}

		entries.add(WatchlistEntry(itemIdStr, serverIdStr))
		saveWatchlistEntries(entries)
		Timber.d("Added item $itemId to local watchlist")
		return true
	}

	/**
	 * Remove an item from the watchlist.
	 */
	fun removeFromWatchlist(itemId: UUID, serverId: UUID): Boolean {
		val entries = getWatchlistEntries().toMutableList()
		val itemIdStr = itemId.toString()
		val serverIdStr = serverId.toString()

		val removed = entries.removeAll { it.itemId == itemIdStr && it.serverId == serverIdStr }
		if (removed) {
			saveWatchlistEntries(entries)
			Timber.d("Removed item $itemId from local watchlist")
		}
		return removed
	}

	fun isInWatchlist(itemId: UUID, serverId: UUID): Boolean {
		val itemIdStr = itemId.toString()
		val serverIdStr = serverId.toString()
		return getWatchlistEntries().any { it.itemId == itemIdStr && it.serverId == serverIdStr }
	}

	fun getWatchlistForServer(serverId: UUID): List<WatchlistEntry> {
		val serverIdStr = serverId.toString()
		return getWatchlistEntries().filter { it.serverId == serverIdStr }
	}

	fun getWatchlistItemIds(serverId: UUID): List<UUID> {
		return getWatchlistForServer(serverId)
			.sortedByDescending { it.addedAt }
			.mapNotNull { entry ->
				try {
					UUID.fromString(entry.itemId)
				} catch (e: Exception) {
					null
				}
			}
	}

	/**
	 * Fetch full BaseItemDto objects for the watchlist items from the server.
	 */
	suspend fun getWatchlistItems(api: ApiClient, serverId: UUID): List<BaseItemDto> = withContext(Dispatchers.IO) {
		val itemIds = getWatchlistItemIds(serverId)
		if (itemIds.isEmpty()) {
			return@withContext emptyList()
		}

		try {
			val response = api.itemsApi.getItems(
				ids = itemIds,
				fields = ItemRepository.itemFields
			)
			
			val itemsById = response.content.items.orEmpty().associateBy { it.id }
			itemIds.mapNotNull { id -> itemsById[id] }
		} catch (e: Exception) {
			Timber.e(e, "Failed to fetch watchlist items from server")
			emptyList()
		}
	}

	fun clearWatchlist() {
		saveWatchlistEntries(emptyList())
		Timber.d("Cleared local watchlist")
	}

	fun clearWatchlistForServer(serverId: UUID) {
		val serverIdStr = serverId.toString()
		val entries = getWatchlistEntries().filterNot { it.serverId == serverIdStr }
		saveWatchlistEntries(entries)
		Timber.d("Cleared local watchlist for server $serverId")
	}
}
