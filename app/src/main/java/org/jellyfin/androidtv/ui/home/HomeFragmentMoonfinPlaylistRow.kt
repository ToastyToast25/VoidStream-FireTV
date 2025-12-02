package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.playback.MoonfinPlaylistManager
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import java.util.UUID

class HomeFragmentMoonfinPlaylistRow(
	private val context: Context,
	private val playlistId: UUID,
	private val api: ApiClient
) : HomeFragmentRow {
	private var row: ListRow? = null
	private var listRowAdapter: MutableObjectAdapter<Any>? = null

	override fun addToRowsAdapter(
		context: Context,
		cardPresenter: CardPresenter,
		rowsAdapter: MutableObjectAdapter<Row>
	) {
		listRowAdapter = MutableObjectAdapter(cardPresenter)
		val header = HeaderItem("Watch List")
		row = ListRow(header, listRowAdapter!!)
		rowsAdapter.add(row!!)

		// Load playlist items asynchronously
		CoroutineScope(Dispatchers.Main).launch {
			loadPlaylistItems(listRowAdapter!!)
		}
	}

	fun refresh() {
		listRowAdapter?.let { adapter ->
			CoroutineScope(Dispatchers.Main).launch {
				// Clear existing items
				adapter.clear()
				// Reload items
				loadPlaylistItems(adapter)
			}
		}
	}

	private suspend fun loadPlaylistItems(adapter: MutableObjectAdapter<Any>) {
		// Load items on IO thread
		val items = withContext(Dispatchers.IO) {
			val playlistManager = MoonfinPlaylistManager(api)
			playlistManager.getMoonfinPlaylistItems()
		}
		
		// Add to adapter on Main thread
		items.forEach { item ->
			adapter.add(BaseItemDtoBaseRowItem(item))
		}
	}
}

