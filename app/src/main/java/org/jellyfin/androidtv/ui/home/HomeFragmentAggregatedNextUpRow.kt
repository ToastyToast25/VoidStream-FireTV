package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.constant.HomeSectionType
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.data.repository.ParentalControlsRepository
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.itemhandling.AggregatedItemRowAdapter
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Home row that displays Next Up items aggregated from all logged-in servers.
 * Items are sorted by premiere date across all servers.
 * Supports pagination - loads 15 items initially, then more as the user scrolls.
 */
class HomeFragmentAggregatedNextUpRow(
	private val maxItems: Int = AggregatedItemRowAdapter.MAX_ITEMS,
) : HomeFragmentRow, KoinComponent {
	private val multiServerRepository by inject<MultiServerRepository>()
	private val userPreferences by inject<UserPreferences>()
	private val userSettingPreferences by inject<UserSettingPreferences>()
	private val parentalControlsRepository by inject<ParentalControlsRepository>()

	override fun addToRowsAdapter(context: Context, cardPresenter: CardPresenter, rowsAdapter: MutableObjectAdapter<Row>) {
		// Create presenter with correct image type from settings
		val imageType = userSettingPreferences.getHomeRowImageType(HomeSectionType.NEXT_UP)
		val presenter = CardPresenter(true, imageType, 150)
		val header = HeaderItem(context.getString(R.string.lbl_next_up))
		
		// Create a placeholder row that will be updated
		val placeholderAdapter = MutableObjectAdapter<Any>(presenter)
		val row = ListRow(header, placeholderAdapter)
		rowsAdapter.add(row)

		// Load items asynchronously
		val lifecycleOwner = ProcessLifecycleOwner.get()
		lifecycleOwner.lifecycleScope.launch {
			try {
				val items = withContext(Dispatchers.IO) {
					multiServerRepository.getAggregatedNextUpItems(maxItems)
				}

				Timber.d("HomeFragmentAggregatedNextUpRow: Loaded ${items.size} next up items from multiple servers")

				if (items.isEmpty()) {
					rowsAdapter.remove(row)
					return@launch
				}

				// Create paginating adapter with all items (filtering happens inside)
				val preferParentThumb = userPreferences[UserPreferences.seriesThumbnailsEnabled]
				val adapter = AggregatedItemRowAdapter(
					presenter = presenter,
					allItems = items,
					parentalControlsRepository = parentalControlsRepository,
					userPreferences = userPreferences,
					chunkSize = AggregatedItemRowAdapter.DEFAULT_CHUNK_SIZE,
					preferParentThumb = preferParentThumb,
					staticHeight = true
				)

				if (!adapter.hasItems()) {
					rowsAdapter.remove(row)
					return@launch
				}

				// Load initial chunk
				adapter.loadInitialItems()
				Timber.d("HomeFragmentAggregatedNextUpRow: Initial load complete, showing ${adapter.size()}/${adapter.getTotalItems()} items")

				// Replace placeholder with real adapter
				val index = rowsAdapter.indexOf(row)
				if (index >= 0) {
					rowsAdapter.removeAt(index, 1)
					rowsAdapter.add(index, ListRow(header, adapter))
				}
			} catch (e: Exception) {
				Timber.e(e, "HomeFragmentAggregatedNextUpRow: Error loading next up items")
				rowsAdapter.remove(row)
			}
		}
	}
}
