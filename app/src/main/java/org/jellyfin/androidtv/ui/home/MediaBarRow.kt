package org.jellyfin.androidtv.ui.home

import androidx.leanback.widget.Row
import org.jellyfin.androidtv.ui.home.mediabar.MediaBarSlideItem

/**
 * Custom Row for the Media Bar slideshow.
 * This row contains a list of slides to be displayed in a full-width slideshow format.
 */
class MediaBarRow(
	val slides: List<MediaBarSlideItem>
) : Row()
