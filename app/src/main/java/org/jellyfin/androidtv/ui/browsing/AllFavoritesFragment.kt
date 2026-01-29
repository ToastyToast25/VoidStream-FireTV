package org.jellyfin.androidtv.ui.browsing

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.util.sdk.ApiClientFactory
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.android.ext.android.inject
import timber.log.Timber

class AllFavoritesFragment : EnhancedBrowseFragment() {
	private val userViewsRepository by inject<UserViewsRepository>()
	private val multiServerRepository by inject<MultiServerRepository>()
	private val userPreferences by inject<UserPreferences>()
	private val apiClientFactory by inject<ApiClientFactory>()
	private val api by inject<ApiClient>()

	init {
		showViews = false
	}

	override fun setupQueries(rowLoader: RowLoader) {
		lifecycleScope.launch {
			val enableMultiServer = userPreferences[UserPreferences.enableMultiServerLibraries]

			if (enableMultiServer) {
				loadMultiServerFavorites(rowLoader)
			} else {
				loadSingleServerFavorites(rowLoader)
			}
		}
	}

	private suspend fun loadMultiServerFavorites(rowLoader: RowLoader) {
		try {
			val aggregatedLibraries = withContext(Dispatchers.IO) {
				multiServerRepository.getAggregatedLibraries()
			}

			val rows = mutableListOf<BrowseRowDef>()

			for (aggLib in aggregatedLibraries) {
				val libraryId = aggLib.library.id ?: continue
				val targetApi = apiClientFactory.getApiClientForServer(aggLib.server.id) ?: api

				val itemsResponse = withContext(Dispatchers.IO) {
					targetApi.itemsApi.getItems(
						parentId = libraryId,
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						fields = ItemRepository.itemFields,
						limit = 1,
					)
				}

				val totalCount = itemsResponse.content.totalRecordCount ?: 0
				if (totalCount > 0) {
					val itemsRequest = GetItemsRequest(
						parentId = libraryId,
						sortBy = setOf(ItemSortBy.SORT_NAME),
						filters = setOf(ItemFilter.IS_FAVORITE),
						includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
						recursive = true,
						fields = ItemRepository.itemFields,
					)
					val browseRowDef = BrowseRowDef(aggLib.displayName, itemsRequest, 40)
					browseRowDef.serverId = aggLib.server.id
					rows.add(browseRowDef)
				}
			}

			rowLoader.loadRows(rows)
		} catch (e: Exception) {
			Timber.e(e, "Failed to load multi-server favorites")
		}
	}

	private suspend fun loadSingleServerFavorites(rowLoader: RowLoader) {
		userViewsRepository.views.collect { userViews ->
			val rows = mutableListOf<BrowseRowDef>()

			for (view in userViews) {
				val viewId = view.id ?: continue
				val itemsRequest = GetItemsRequest(
					parentId = viewId,
					sortBy = setOf(ItemSortBy.SORT_NAME),
					filters = setOf(ItemFilter.IS_FAVORITE),
					includeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
					recursive = true,
					fields = ItemRepository.itemFields,
				)
				rows.add(BrowseRowDef(view.name ?: "Library", itemsRequest, 40))
			}

			rowLoader.loadRows(rows)
		}
	}
}
