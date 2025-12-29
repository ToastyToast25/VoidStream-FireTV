package org.jellyfin.androidtv.ui.preference.screen

import android.content.Intent
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.AppTheme
import org.jellyfin.androidtv.preference.constant.ClockBehavior
import org.jellyfin.androidtv.preference.constant.RatingType
import org.jellyfin.androidtv.preference.constant.WatchedIndicatorBehavior
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.preference.PreferencesActivity
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.koin.compose.koinInject

@Composable
fun CustomizationPreferencesScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	val context = LocalContext.current

	SettingsColumn {
		// Theme section
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_theme)) },
			)
		}

		item {
			var appTheme by rememberPreference(userPreferences, UserPreferences.appTheme)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_app_theme)) },
				captionContent = { Text(stringResource(appTheme.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_APP_THEME) }
			)
		}

		item {
			var clockBehavior by rememberPreference(userPreferences, UserPreferences.clockBehavior)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_clock_display)) },
				captionContent = { Text(stringResource(clockBehavior.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_CLOCK_BEHAVIOR) }
			)
		}

		item {
			var watchedIndicatorBehavior by rememberPreference(userPreferences, UserPreferences.watchedIndicatorBehavior)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_watched_indicator)) },
				captionContent = { Text(stringResource(watchedIndicatorBehavior.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_WATCHED_INDICATOR_BEHAVIOR) }
			)
		}

		item {
			var backdropEnabled by rememberPreference(userPreferences, UserPreferences.backdropEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_show_backdrop)) },
				captionContent = { Text(stringResource(R.string.pref_show_backdrop_description)) },
				trailingContent = { Checkbox(checked = backdropEnabled) },
				onClick = { backdropEnabled = !backdropEnabled }
			)
		}

		item {
			var seriesThumbnailsEnabled by rememberPreference(userPreferences, UserPreferences.seriesThumbnailsEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_use_series_thumbnails)) },
				captionContent = { Text(stringResource(R.string.lbl_use_series_thumbnails_description)) },
				trailingContent = { Checkbox(checked = seriesThumbnailsEnabled) },
				onClick = { seriesThumbnailsEnabled = !seriesThumbnailsEnabled }
			)
		}

		item {
			var defaultRatingType by rememberPreference(userPreferences, UserPreferences.defaultRatingType)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_default_rating)) },
				captionContent = { Text(stringResource(defaultRatingType.nameRes)) },
				onClick = { router.push(Routes.CUSTOMIZATION_RATING_TYPE) }
			)
		}

		// Browsing section
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.pref_browsing)) },
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.home_prefs)) },
				captionContent = { Text(stringResource(R.string.pref_home_description)) },
				onClick = { router.push(Routes.HOME_SECTIONS) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_grid), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_libraries)) },
				captionContent = { Text(stringResource(R.string.pref_libraries_description)) },
				onClick = {
					val intent = Intent(context, PreferencesActivity::class.java).apply {
						putExtra(PreferencesActivity.EXTRA_SCREEN, LibrariesPreferencesScreen::class.qualifiedName)
						putExtra(PreferencesActivity.EXTRA_SCREEN_ARGS, android.os.Bundle())
					}
					context.startActivity(intent)
				}
			)
		}

		// Screensaver section
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.pref_screensaver)) },
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
			)
		}

		item {
			var screensaverInAppEnabled by rememberPreference(userPreferences, UserPreferences.screensaverInAppEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_inapp_enabled)) },
				captionContent = { Text(stringResource(R.string.pref_screensaver_inapp_enabled_description)) },
				trailingContent = { Checkbox(checked = screensaverInAppEnabled) },
				onClick = { screensaverInAppEnabled = !screensaverInAppEnabled }
			)
		}

		item {
			var screensaverTimeout by rememberPreference(userPreferences, UserPreferences.screensaverInAppTimeout)
			val timeoutMinutes = screensaverTimeout / 60000L
			val timeoutDisplay = when (timeoutMinutes) {
				0L -> "30 seconds"
				1L -> "1 minute"
				3L -> "2.5 minutes"
				5L -> "5 minutes"
				10L -> "10 minutes"
				15L -> "15 minutes"
				30L -> "30 minutes"
				else -> "$timeoutMinutes min"
			}
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_inapp_timeout)) },
				captionContent = { Text(timeoutDisplay) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER_TIMEOUT) }
			)
		}

		item {
			var screensaverAgeRatingRequired by rememberPreference(userPreferences, UserPreferences.screensaverAgeRatingRequired)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_ageratingrequired_title)) },
				captionContent = { 
					Text(stringResource(
						if (screensaverAgeRatingRequired) R.string.pref_screensaver_ageratingrequired_enabled 
						else R.string.pref_screensaver_ageratingrequired_disabled
					))
				},
				trailingContent = { Checkbox(checked = screensaverAgeRatingRequired) },
				onClick = { screensaverAgeRatingRequired = !screensaverAgeRatingRequired }
			)
		}

		item {
			var screensaverAgeRatingMax by rememberPreference(userPreferences, UserPreferences.screensaverAgeRatingMax)
			val ageDisplay = when (screensaverAgeRatingMax) {
				0 -> stringResource(R.string.pref_screensaver_ageratingmax_zero)
				-1 -> stringResource(R.string.pref_screensaver_ageratingmax_unlimited)
				else -> stringResource(R.string.pref_screensaver_ageratingmax_entry, screensaverAgeRatingMax)
			}
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_screensaver_ageratingmax)) },
				captionContent = { Text(ageDisplay) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER_AGE_RATING) }
			)
		}

		// Behavior section
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.pref_behavior)) },
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
			)
		}

		// Note: Audio and subtitle track button shortcuts are not included here
		// as they require special key capture dialogs that are implemented
		// in the old preference system (ButtonRemapPreference).
		// Users can access these settings through the MoonfinPreferencesScreen
		// (Settings > Moonfin Settings) which uses the fragment-based system.
	}
}
