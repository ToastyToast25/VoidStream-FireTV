package org.jellyfin.androidtv.ui.browsing.genre

import java.util.UUID

data class JellyfinGenreItem(
	val id: UUID,
	val name: String,
	val backdropUrl: String? = null,
	val itemCount: Int = 0,
	val parentId: UUID? = null,
	val serverId: UUID? = null,
)
