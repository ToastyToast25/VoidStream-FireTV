package org.jellyfin.androidtv.util.apiclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.Response

/**
 * Execute an API call on the IO dispatcher.
 * This extension function wraps API calls in withContext(Dispatchers.IO) to ensure
 * they run on the appropriate background thread.
 *
 * @param block The API call to execute
 * @return The result of the API call
 *
 * Usage example:
 * ```
 * val items = api.ioCall { itemsApi.getItems(request).content.items }
 * ```
 */
suspend fun <T> ApiClient.ioCall(block: suspend ApiClient.() -> T): T =
	withContext(Dispatchers.IO) { block() }

/**
 * Execute an API call that returns a Response on the IO dispatcher and extract the content.
 * This is a convenience function for the common pattern of calling an API and immediately
 * accessing the .content property.
 *
 * @param block The API call to execute that returns a Response
 * @return The content of the Response
 *
 * Usage example:
 * ```
 * val items = api.ioCallContent { itemsApi.getItems(request) }
 * // Equivalent to: withContext(Dispatchers.IO) { api.itemsApi.getItems(request).content }
 * ```
 */
suspend fun <T> ApiClient.ioCallContent(block: suspend ApiClient.() -> Response<T>): T =
	withContext(Dispatchers.IO) { block().content }
