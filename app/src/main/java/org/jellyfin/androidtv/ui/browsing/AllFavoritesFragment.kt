package org.jellyfin.androidtv.ui.browsing

import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.ui.itemhandling.ItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.personsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.koin.android.ext.android.inject
import timber.log.Timber

class AllFavoritesFragment : EnhancedBrowseFragment() {
	private val api by inject<ApiClient>()

	private var favoriteCastItems: List<BaseItemDto>? = null
	private var favoriteMovieItems: List<BaseItemDto>? = null
	private var favoriteShowItems: List<BaseItemDto>? = null
	private var favoriteEpisodeItems: List<BaseItemDto>? = null
	private var favoritePlaylistItems: List<BaseItemDto>? = null

	init {
		showViews = false
	}

	override fun setupQueries(rowLoader: RowLoader) {
		lifecycleScope.launch {
			loadFavorites(rowLoader)
		}
	}

	override fun loadRows(rows: MutableList<BrowseRowDef>) {
		super.loadRows(rows)

		val cardPresenter = CardPresenter(false, 140)

		// Insert in reverse order since adding at position 0
		addFavoriteRow(favoritePlaylistItems, "Favorite Playlists", cardPresenter)
		addFavoriteRow(favoriteEpisodeItems, "Favorite Episodes", cardPresenter)
		addFavoriteRow(favoriteShowItems, "Favorite Shows", cardPresenter)
		addFavoriteRow(favoriteMovieItems, "Favorite Movies", cardPresenter)
		addFavoriteRow(favoriteCastItems, "Favorite Cast", cardPresenter)
	}

	private fun addFavoriteRow(items: List<BaseItemDto>?, title: String, cardPresenter: CardPresenter) {
		items?.takeIf { it.isNotEmpty() }?.let {
			val rowAdapter = ItemRowAdapter(requireContext(), it, cardPresenter, mRowsAdapter, true)
			val row = ListRow(HeaderItem(title), rowAdapter)
			rowAdapter.setRow(row)
			mRowsAdapter.add(0, row)
			rowAdapter.Retrieve()
		}
	}

	private suspend fun loadFavorites(rowLoader: RowLoader) {
		try {
			withContext(Dispatchers.IO) {
				val castDeferred = async {
					api.personsApi.getPersons(
						isFavorite = true,
						fields = ItemRepository.itemFields,
					).content.items
				}

				val moviesDeferred = async {
					api.itemsApi.getItems(
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.MOVIE),
						recursive = true,
						fields = ItemRepository.itemFields,
					).content.items
				}

				val showsDeferred = async {
					api.itemsApi.getItems(
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.SERIES),
						recursive = true,
						fields = ItemRepository.itemFields,
					).content.items
				}

				val episodesDeferred = async {
					api.itemsApi.getItems(
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.EPISODE),
						recursive = true,
						fields = ItemRepository.itemFields,
					).content.items
				}

				val playlistsDeferred = async {
					api.itemsApi.getItems(
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.PLAYLIST),
						recursive = true,
						fields = ItemRepository.itemFields,
					).content.items
				}

				awaitAll(castDeferred, moviesDeferred, showsDeferred, episodesDeferred, playlistsDeferred)

				favoriteCastItems = castDeferred.await()
				favoriteMovieItems = moviesDeferred.await()
				favoriteShowItems = showsDeferred.await()
				favoriteEpisodeItems = episodesDeferred.await()
				favoritePlaylistItems = playlistsDeferred.await()
			}

			rowLoader.loadRows(mutableListOf())
		} catch (e: Exception) {
			Timber.e(e, "Failed to load favorites")
		}
	}
}
