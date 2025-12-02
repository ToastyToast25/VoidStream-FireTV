package org.jellyfin.androidtv.ui.playback

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CreatePlaylistDto
import timber.log.Timber
import java.util.UUID

class MoonfinPlaylistManager(
	private val api: ApiClient
) {
	companion object {
		const val MOONFIN_PLAYLIST_NAME = "Moonfin"
	}

	/**
	 * Get or create the Moonfin playlist
	 */
	suspend fun getOrCreateMoonfinPlaylist(): UUID? {
		return try {
			// Search for existing Moonfin playlist
			val response = api.itemsApi.getItems(
				includeItemTypes = setOf(BaseItemKind.PLAYLIST),
				recursive = true,
				searchTerm = MOONFIN_PLAYLIST_NAME
			)

			val existingPlaylist = response.content.items?.firstOrNull {
				it.name.equals(MOONFIN_PLAYLIST_NAME, ignoreCase = true)
			}

			val playlistId: UUID? = if (existingPlaylist != null) {
				Timber.d("Found existing Moonfin playlist: ${existingPlaylist.id}")
				existingPlaylist.id as? UUID
			} else {
				// Create new playlist
				Timber.d("Creating new Moonfin playlist")
				val newPlaylist = api.playlistsApi.createPlaylist(
					data = CreatePlaylistDto(
						name = MOONFIN_PLAYLIST_NAME,
						ids = emptyList(),
						users = emptyList(),
						isPublic = true
					)
				)
				UUID.fromString(newPlaylist.content.id)
			}
			playlistId
		} catch (e: Exception) {
			Timber.e(e, "Failed to get or create Moonfin playlist")
			null
		}
	}

	/**
	 * Add an item to the Moonfin playlist
	 */
	suspend fun addToMoonfinPlaylist(itemId: UUID): Boolean {
		return try {
			val playlistId = getOrCreateMoonfinPlaylist()
			if (playlistId != null) {
				api.playlistsApi.addItemToPlaylist(
					playlistId = playlistId,
					ids = listOf(itemId)
				)
				Timber.d("Added item $itemId to Moonfin playlist")
				true
			} else {
				Timber.w("Could not get or create Moonfin playlist")
				false
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to add item to Moonfin playlist")
			false
		}
	}

	/**
	 * Get items in the Moonfin playlist
	 */
	suspend fun getMoonfinPlaylistItems(): List<org.jellyfin.sdk.model.api.BaseItemDto> {
		return try {
			val playlistId = getOrCreateMoonfinPlaylist()
			if (playlistId != null) {
				val response = api.playlistsApi.getPlaylistItems(
					playlistId = playlistId,
					fields = org.jellyfin.androidtv.data.repository.ItemRepository.itemFields
				)
				response.content.items.orEmpty()
			} else {
				emptyList()
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to get Moonfin playlist items")
			emptyList()
		}
	}

	/**
	 * Check if an item is in the Moonfin playlist
	 */
	suspend fun isItemInPlaylist(itemId: UUID): Boolean {
		return try {
			val items = getMoonfinPlaylistItems()
			items.any { it.id?.toString() == itemId.toString() }
		} catch (e: Exception) {
			Timber.e(e, "Failed to check if item is in Moonfin playlist")
			false
		}
	}

	/**
	 * Remove an item from the Moonfin playlist
	 */
	suspend fun removeFromMoonfinPlaylist(itemId: UUID): Boolean {
		return try {
			val playlistId = getOrCreateMoonfinPlaylist()
			if (playlistId != null) {
				// Get all playlist items to find the entry ID(s) for this item
				val playlistItems = api.playlistsApi.getPlaylistItems(
					playlistId = playlistId,
					fields = org.jellyfin.androidtv.data.repository.ItemRepository.itemFields
				).content.items.orEmpty()
				
				// Find all entries that match this item ID
				val entryIds = playlistItems
					.filter { it.id?.toString() == itemId.toString() }
					.mapNotNull { it.playlistItemId?.toString() }
				
				if (entryIds.isNotEmpty()) {
					// Remove using playlist entry IDs as strings
					api.playlistsApi.removeItemFromPlaylist(
						playlistId = playlistId.toString(),
						entryIds = entryIds
					)
					Timber.d("Removed item $itemId from Moonfin playlist (entry IDs: $entryIds)")
					true
				} else {
					Timber.w("Item $itemId not found in Moonfin playlist")
					false
				}
			} else {
				Timber.w("Could not get or create Moonfin playlist")
				false
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to remove item from Moonfin playlist")
			false
		}
	}
}
