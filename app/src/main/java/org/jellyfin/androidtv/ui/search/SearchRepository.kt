package org.jellyfin.androidtv.ui.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.constant.QueryDefaults
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.util.apiclient.ioCallContent
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.personsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetPersonsRequest
import timber.log.Timber
import java.util.UUID

interface SearchRepository {
	suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>>

	suspend fun searchMultiServer(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>>
}

class SearchRepositoryImpl(
	private val apiClient: ApiClient,
	private val multiServerRepository: MultiServerRepository,
) : SearchRepository {

	private fun BaseItemDto.withServerId(serverId: UUID): BaseItemDto =
		copy(serverId = serverId.toString())

	override suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>> = try {
		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.PERSON) {
			val request = GetPersonsRequest(
				searchTerm = searchTerm,
				limit = QueryDefaults.SEARCH_PAGE_SIZE,
				imageTypeLimit = 1,
				fields = ItemRepository.itemFields,
			)

			val result = apiClient.ioCallContent {
				personsApi.getPersons(request)
			}

			return Result.success(result.items)
		}

		var request = GetItemsRequest(
			searchTerm = searchTerm,
			limit = QueryDefaults.SEARCH_PAGE_SIZE,
			imageTypeLimit = 1,
			includeItemTypes = itemTypes,
			fields = ItemRepository.itemFields,
			recursive = true,
			enableTotalRecordCount = false,
		)

		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.VIDEO) {
			request = request.copy(
				mediaTypes = setOf(MediaType.VIDEO),
				includeItemTypes = null,
				excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.TV_CHANNEL)
			)
		}

		val result = apiClient.ioCallContent {
			itemsApi.getItems(request)
		}

		Result.success(result.items)
	} catch (e: ApiClientException) {
		Timber.e(e, "Failed to search for items")
		Result.failure(e)
	}

	override suspend fun searchMultiServer(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>> = withContext(Dispatchers.IO) {
		try {
			val sessions = multiServerRepository.getLoggedInServers()
			Timber.d("SearchRepository: Multi-server search across ${sessions.size} servers for '$searchTerm'")

			if (sessions.isEmpty()) {
				return@withContext search(searchTerm, itemTypes)
			}

			val allResults = sessions.map { session ->
				async {
					try {
						val items = searchOnServer(session.apiClient, searchTerm, itemTypes)
						items.map { it.withServerId(session.server.id) }
					} catch (e: Exception) {
						Timber.e(e, "SearchRepository: Failed to search server ${session.server.name}")
						emptyList()
					}
				}
			}.awaitAll().flatten()

			Timber.d("SearchRepository: Found ${allResults.size} total results across all servers")
			Result.success(allResults)
		} catch (e: Exception) {
			Timber.e(e, "SearchRepository: Multi-server search failed")
			Result.failure(e)
		}
	}

	private suspend fun searchOnServer(
		client: ApiClient,
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): List<BaseItemDto> {
		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.PERSON) {
			val request = GetPersonsRequest(
				searchTerm = searchTerm,
				limit = QueryDefaults.SEARCH_PAGE_SIZE,
				imageTypeLimit = 1,
				fields = ItemRepository.itemFields,
			)

			val result = client.ioCallContent {
				personsApi.getPersons(request)
			}

			return result.items
		}

		var request = GetItemsRequest(
			searchTerm = searchTerm,
			limit = QueryDefaults.SEARCH_PAGE_SIZE,
			imageTypeLimit = 1,
			includeItemTypes = itemTypes,
			fields = ItemRepository.itemFields,
			recursive = true,
			enableTotalRecordCount = false,
		)

		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.VIDEO) {
			request = request.copy(
				mediaTypes = setOf(MediaType.VIDEO),
				includeItemTypes = null,
				excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.TV_CHANNEL)
			)
		}

		val result = client.ioCallContent {
			itemsApi.getItems(request)
		}

		return result.items
	}
}
