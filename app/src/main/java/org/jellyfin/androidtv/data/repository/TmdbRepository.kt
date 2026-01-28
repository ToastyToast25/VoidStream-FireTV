package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID

@Serializable
data class TmdbEpisodeResponse(
	val id: Int? = null,
	val name: String? = null,
	val vote_average: Float? = null,
	val vote_count: Int? = null,
	val season_number: Int? = null,
	val episode_number: Int? = null,
	val air_date: String? = null
)

@Serializable
data class TmdbSeasonResponse(
	val id: Int? = null,
	val name: String? = null,
	val episodes: List<TmdbEpisodeResponse>? = null
)

class TmdbRepository(
	private val okHttpClient: OkHttpClient,
	private val apiClient: ApiClient,
) {
	private val episodeRatingsCache = mutableMapOf<String, Float>()
	private val seasonCache = mutableMapOf<String, Map<Int, Float>>()
	private val seriesTmdbIdCache = mutableMapOf<String, String?>()
	private val pendingRequests = mutableMapOf<String, CompletableDeferred<Float?>>()
	private val pendingSeasonRequests = mutableMapOf<String, CompletableDeferred<Map<Int, Float>?>>()

	private val json = Json {
		ignoreUnknownKeys = true
		isLenient = true
	}

	suspend fun getEpisodeRating(item: BaseItemDto, apiKey: String): Float? = withContext(Dispatchers.IO) {
		if (apiKey.isBlank()) {
			Timber.d("TMDB API key is blank, skipping episode rating fetch")
			return@withContext null
		}
		if (item.type != BaseItemKind.EPISODE) {
			Timber.d("Item ${item.name} is not an episode, skipping")
			return@withContext null
		}

		val seriesId = item.seriesId
		if (seriesId == null) {
			Timber.w("Episode ${item.name} has no seriesId")
			return@withContext null
		}

		val tmdbId = getSeriesTmdbId(seriesId)
		if (tmdbId == null) {
			Timber.w("Could not get TMDB ID for series ${item.seriesName} (${seriesId})")
			return@withContext null
		}

		val seasonNumber = item.parentIndexNumber
		val episodeNumber = item.indexNumber
		if (seasonNumber == null || episodeNumber == null) {
			Timber.w("Episode ${item.name} missing season/episode numbers: S${seasonNumber}E${episodeNumber}")
			return@withContext null
		}

		val cacheKey = "$tmdbId:$seasonNumber:$episodeNumber"
		Timber.d("Fetching TMDB EPISODE rating for ${item.seriesName} S${seasonNumber}E${episodeNumber} (using series TMDB ID: $tmdbId)")

		episodeRatingsCache[cacheKey]?.let {
			Timber.d("Cache hit for episode $cacheKey: $it")
			return@withContext it
		}
		pendingRequests[cacheKey]?.let {
			Timber.d("Awaiting pending episode request for $cacheKey")
			return@withContext it.await()
		}

		val deferred = CompletableDeferred<Float?>()
		pendingRequests[cacheKey] = deferred

		try {
			val url = "https://api.themoviedb.org/3/tv/$tmdbId/season/$seasonNumber/episode/$episodeNumber"
			val requestBuilder = Request.Builder().url(url)
			
			if (apiKey.startsWith("eyJ")) {
				requestBuilder.addHeader("Authorization", "Bearer $apiKey")
			} else {
				requestBuilder.url("$url?api_key=$apiKey")
			}
			
			val request = requestBuilder.build()
			val response = okHttpClient.newCall(request).execute()

			val result = if (response.isSuccessful) {
				Timber.d("TMDB API response successful for episode $cacheKey")
				val body = response.body?.string()
				if (body != null) {
					try {
						val episodeResponse = json.decodeFromString<TmdbEpisodeResponse>(body)
						val rating = episodeResponse.vote_average
						Timber.d("Parsed EPISODE response: name='${episodeResponse.name}', rating=$rating, vote_count=${episodeResponse.vote_count}")
						if (rating != null && rating > 0f) {
							episodeRatingsCache[cacheKey] = rating
							Timber.i("Successfully cached EPISODE TMDB rating for $cacheKey: $rating/10")
							rating
						} else {
							Timber.d("No valid rating for episode $cacheKey (rating=$rating)")
							null
						}
					} catch (e: Exception) {
						Timber.w(e, "Failed to parse TMDB episode response for $cacheKey. Body: ${body.take(200)}")
						null
					}
				} else {
					Timber.w("TMDB API returned empty body for episode $cacheKey")
					null
				}
			} else {
				val errorBody = response.body?.string()
				Timber.w("TMDB API request failed for episode $cacheKey: ${response.code} ${response.message}. Error: ${errorBody?.take(200)}")
				null
			}
			deferred.complete(result)
			return@withContext result
		} catch (e: Exception) {
			Timber.e(e, "Error fetching TMDB episode rating for $cacheKey")
			deferred.complete(null)
			return@withContext null
		} finally {
			pendingRequests.remove(cacheKey)
		}
	}

	suspend fun getSeasonEpisodeRatings(
		seriesTmdbId: String,
		seasonNumber: Int,
		apiKey: String
	): Map<Int, Float>? = withContext(Dispatchers.IO) {
		if (apiKey.isBlank()) return@withContext null

		val cacheKey = "$seriesTmdbId:$seasonNumber"

		seasonCache[cacheKey]?.let { return@withContext it }
		pendingSeasonRequests[cacheKey]?.let { return@withContext it.await() }

		val deferred = CompletableDeferred<Map<Int, Float>?>()
		pendingSeasonRequests[cacheKey] = deferred

		try {
			val url = "https://api.themoviedb.org/3/tv/$seriesTmdbId/season/$seasonNumber"
			val requestBuilder = Request.Builder().url(url)
			
			if (apiKey.startsWith("eyJ")) {
				requestBuilder.addHeader("Authorization", "Bearer $apiKey")
			} else {
				requestBuilder.url("$url?api_key=$apiKey")
			}
			
			val request = requestBuilder.build()
			val response = okHttpClient.newCall(request).execute()

			val result = if (response.isSuccessful) {
				val body = response.body?.string()
				if (body != null) {
					try {
						val seasonResponse = json.decodeFromString<TmdbSeasonResponse>(body)
						val ratingsMap = seasonResponse.episodes
							?.filter { it.vote_average != null && it.vote_average > 0f && it.episode_number != null }
							?.associate { it.episode_number!! to it.vote_average!! }
							?: emptyMap()

						if (ratingsMap.isNotEmpty()) {
							seasonCache[cacheKey] = ratingsMap
							ratingsMap.forEach { (epNum, rating) ->
								episodeRatingsCache["$seriesTmdbId:$seasonNumber:$epNum"] = rating
							}
						}
						ratingsMap
					} catch (e: Exception) {
						Timber.w(e, "Failed to parse TMDB season response for $cacheKey")
						null
					}
				} else null
			} else {
				Timber.w("TMDB API request failed for $cacheKey: ${response.code}")
				null
			}
			deferred.complete(result)
			return@withContext result
		} catch (e: Exception) {
			Timber.e(e, "Error fetching TMDB season ratings for $cacheKey")
			deferred.complete(null)
			return@withContext null
		} finally {
			pendingSeasonRequests.remove(cacheKey)
		}
	}

	private suspend fun getSeriesTmdbId(seriesId: UUID): String? {
		val cacheKey = seriesId.toString()
		seriesTmdbIdCache[cacheKey]?.let { return it }
		
		try {
			Timber.d("Fetching series info from Jellyfin for seriesId: $seriesId")
			val response = apiClient.userLibraryApi.getItem(itemId = seriesId)
			val seriesItem = response.content
			
			val tmdbId = seriesItem.providerIds?.get("Tmdb")
			if (tmdbId != null) {
				Timber.i("Found TMDB ID for series ${seriesItem.name}: $tmdbId")
				seriesTmdbIdCache[cacheKey] = tmdbId
				return tmdbId
			} else {
				Timber.w("Series ${seriesItem.name} has no TMDB provider ID. Available IDs: ${seriesItem.providerIds?.keys}")
				seriesTmdbIdCache[cacheKey] = null
				return null
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to fetch series info for seriesId: $seriesId")
			return null
		}
	}

	fun clearCache() {
		episodeRatingsCache.clear()
		seasonCache.clear()
		seriesTmdbIdCache.clear()
		pendingRequests.clear()
		pendingSeasonRequests.clear()
	}
}
