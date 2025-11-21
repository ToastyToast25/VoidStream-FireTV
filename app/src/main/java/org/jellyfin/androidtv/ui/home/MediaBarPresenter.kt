package org.jellyfin.androidtv.ui.home

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.leanback.widget.RowPresenter
import org.jellyfin.androidtv.ui.home.mediabar.MediaBarSlideshowView
import org.jellyfin.androidtv.ui.home.mediabar.MediaBarSlideshowViewModel
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository

/**
 * Presenter for the Media Bar row.
 * This presenter creates a full-width Compose view for the slideshow.
 */
class MediaBarPresenter(
	private val viewModel: MediaBarSlideshowViewModel,
	private val navigationRepository: NavigationRepository
) : RowPresenter() {
	
	override fun createRowViewHolder(parent: ViewGroup): RowPresenter.ViewHolder {
		val composeView = ComposeView(parent.context).apply {
			layoutParams = ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			
			setContent {
				MediaBarSlideshowView(
					viewModel = viewModel,
					onItemClick = { item ->
						// Navigate to item details
						navigationRepository.navigate(Destinations.itemDetails(item.itemId))
					}
				)
			}
		}
		
		return ViewHolder(composeView)
	}
	
	override fun onBindRowViewHolder(vh: RowPresenter.ViewHolder, item: Any) {
		// Binding is handled by Compose state
	}
	
	override fun onUnbindRowViewHolder(vh: RowPresenter.ViewHolder) {
		// Cleanup is handled by Compose lifecycle
	}
	
	class ViewHolder(view: android.view.View) : RowPresenter.ViewHolder(view)
}
