package org.jellyfin.androidtv.ui.preference.screen

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.constant.JellyseerrFetchLimit
import org.jellyfin.androidtv.data.repository.JellyseerrRepository
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrHttpClient
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrQualityProfileDto
import org.jellyfin.androidtv.preference.JellyseerrPreferences
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.enum
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.jellyfin.sdk.api.client.ApiClient
import org.koin.android.ext.android.inject
import timber.log.Timber

class JellyseerrPreferencesScreen : OptionsFragment() {
	private val jellyseerrPreferences: JellyseerrPreferences by inject()
	private val jellyseerrRepository: JellyseerrRepository by inject()
	private val apiClient: ApiClient by inject()
	private val userRepository: UserRepository by inject()

	override val screen by optionsScreen {
		setTitle(R.string.jellyseerr_settings)

		// Server Configuration
		category {
			setTitle(R.string.jellyseerr_server_settings)

			checkbox {
				setTitle(R.string.jellyseerr_enabled)
				setContent(R.string.jellyseerr_enabled_description)
				bind(jellyseerrPreferences, JellyseerrPreferences.enabled)
			}

			action {
				setTitle(R.string.jellyseerr_server_url)
				setContent(R.string.jellyseerr_server_url_description)
				icon = R.drawable.ic_settings
				onActivate = {
					showServerUrlDialog()
				}
			}
		}

		// Authentication Methods
		category {
			setTitle(R.string.jellyseerr_auth_method)

			action {
				setTitle(R.string.jellyseerr_connect_jellyfin)
				setContent(R.string.jellyseerr_connect_jellyfin_description)
				icon = R.drawable.ic_jellyseerr_jellyfish
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				onActivate = {
					connectWithJellyfin()
				}
			}

			action {
				setTitle(R.string.jellyseerr_login_local)
				setContent(R.string.jellyseerr_login_local_description)
				icon = R.drawable.ic_user
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				onActivate = {
					loginWithLocalAccount()
				}
			}

			action {
				setTitle(R.string.jellyseerr_api_key_status)
				icon = R.drawable.ic_lightbulb
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				// Note: This content is evaluated when the screen loads
				// After logging in, exit and re-enter this screen to see updated status
				content = run {
					val apiKey = jellyseerrPreferences[JellyseerrPreferences.apiKey] ?: ""
					if (apiKey.isNotEmpty()) {
						getString(R.string.jellyseerr_api_key_present)
					} else {
						getString(R.string.jellyseerr_api_key_absent)
					}
				}
			}
		}

		// Content Preferences
		category {
			setTitle(R.string.pref_customization)

			enum<JellyseerrFetchLimit> {
				setTitle(R.string.jellyseerr_fetch_limit_title)
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				bind(jellyseerrPreferences, JellyseerrPreferences.fetchLimit)
			}

			checkbox {
				setTitle(R.string.jellyseerr_block_nsfw)
				setContent(R.string.jellyseerr_block_nsfw_description)
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				bind(jellyseerrPreferences, JellyseerrPreferences.blockNsfw)
			}
		}

		// Advanced Request Settings (admin only)
		category {
			setTitle(R.string.jellyseerr_advanced_settings)

			action {
				setTitle(R.string.jellyseerr_hd_movie_profile)
				setContent(R.string.jellyseerr_hd_movie_profile_description)
				icon = R.drawable.ic_select_quality
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				content = run {
					val profileId = jellyseerrPreferences[JellyseerrPreferences.hdMovieProfileId]
					if (profileId.isNullOrEmpty()) getString(R.string.jellyseerr_profile_default) else "Profile #$profileId"
				}
				onActivate = {
					showProfileSelectionDialog(
						isMovie = true,
						is4k = false,
						currentProfileId = jellyseerrPreferences[JellyseerrPreferences.hdMovieProfileId]
					) { profileId ->
						jellyseerrPreferences[JellyseerrPreferences.hdMovieProfileId] = profileId
					}
				}
			}

			action {
				setTitle(R.string.jellyseerr_4k_movie_profile)
				setContent(R.string.jellyseerr_4k_movie_profile_description)
				icon = R.drawable.ic_4k
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				content = run {
					val profileId = jellyseerrPreferences[JellyseerrPreferences.fourKMovieProfileId]
					if (profileId.isNullOrEmpty()) getString(R.string.jellyseerr_profile_default) else "Profile #$profileId"
				}
				onActivate = {
					showProfileSelectionDialog(
						isMovie = true,
						is4k = true,
						currentProfileId = jellyseerrPreferences[JellyseerrPreferences.fourKMovieProfileId]
					) { profileId ->
						jellyseerrPreferences[JellyseerrPreferences.fourKMovieProfileId] = profileId
					}
				}
			}

			action {
				setTitle(R.string.jellyseerr_hd_tv_profile)
				setContent(R.string.jellyseerr_hd_tv_profile_description)
				icon = R.drawable.ic_select_quality
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				content = run {
					val profileId = jellyseerrPreferences[JellyseerrPreferences.hdTvProfileId]
					if (profileId.isNullOrEmpty()) getString(R.string.jellyseerr_profile_default) else "Profile #$profileId"
				}
				onActivate = {
					showProfileSelectionDialog(
						isMovie = false,
						is4k = false,
						currentProfileId = jellyseerrPreferences[JellyseerrPreferences.hdTvProfileId]
					) { profileId ->
						jellyseerrPreferences[JellyseerrPreferences.hdTvProfileId] = profileId
					}
				}
			}

			action {
				setTitle(R.string.jellyseerr_4k_tv_profile)
				setContent(R.string.jellyseerr_4k_tv_profile_description)
				icon = R.drawable.ic_4k
				depends { jellyseerrPreferences[JellyseerrPreferences.enabled] }
				content = run {
					val profileId = jellyseerrPreferences[JellyseerrPreferences.fourKTvProfileId]
					if (profileId.isNullOrEmpty()) getString(R.string.jellyseerr_profile_default) else "Profile #$profileId"
				}
				onActivate = {
					showProfileSelectionDialog(
						isMovie = false,
						is4k = true,
						currentProfileId = jellyseerrPreferences[JellyseerrPreferences.fourKTvProfileId]
					) { profileId ->
						jellyseerrPreferences[JellyseerrPreferences.fourKTvProfileId] = profileId
					}
				}
			}
		}
	}

	private fun loginWithLocalAccount() {
		Timber.i("Jellyseerr: Starting local account login flow")
		val serverUrl = jellyseerrPreferences[JellyseerrPreferences.serverUrl]
		
		if (serverUrl.isNullOrBlank()) {
			Timber.w("Jellyseerr: Cannot login - server URL not configured")
			Toast.makeText(requireContext(), "Please set server URL first", Toast.LENGTH_SHORT).show()
			return
		}

		Timber.d("Jellyseerr: Showing local login dialog for server: $serverUrl")
		
		// Create input fields
		val emailInput = EditText(requireContext()).apply {
			hint = "Email"
			inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
		}
		
		val passwordInput = EditText(requireContext()).apply {
			hint = "Password"
			inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
		}

		val layout = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(48, 32, 48, 32)
			addView(TextView(requireContext()).apply {
				text = "Login with your Jellyseerr local account to get a permanent API key"
				setPadding(0, 0, 0, 32)
			})
			addView(emailInput)
			addView(passwordInput)
		}

		AlertDialog.Builder(requireContext())
			.setTitle(R.string.jellyseerr_login_local)
			.setView(layout)
			.setPositiveButton("Login") { _, _ ->
				val email = emailInput.text.toString().trim()
				val password = passwordInput.text.toString().trim()
				
				if (email.isEmpty() || password.isEmpty()) {
					Timber.w("Jellyseerr: Login cancelled - empty credentials")
					Toast.makeText(requireContext(), "Email and password are required", Toast.LENGTH_SHORT).show()
					return@setPositiveButton
				}

				Timber.i("Jellyseerr: Attempting local login for email: $email")
				performLocalLogin(serverUrl, email, password)
			}
			.setNegativeButton("Cancel", null)
			.show()
	}

	private fun performLocalLogin(jellyseerrServerUrl: String, email: String, password: String) {
		Timber.d("Jellyseerr: Performing local login to: $jellyseerrServerUrl")
		Toast.makeText(requireContext(), "Logging in...", Toast.LENGTH_SHORT).show()

		lifecycleScope.launch {
			try {
				Timber.d("Jellyseerr: Calling repository.loginLocal()")
				val result = jellyseerrRepository.loginLocal(email, password, jellyseerrServerUrl)
				
				result.onSuccess { user ->
					Timber.i("Jellyseerr: Local login successful - User ID: ${user.id}, Username: ${user.username}")
					jellyseerrPreferences[JellyseerrPreferences.enabled] = true
					jellyseerrPreferences[JellyseerrPreferences.lastConnectionSuccess] = true
					
					val apiKeyLength = jellyseerrPreferences[JellyseerrPreferences.apiKey]?.length ?: 0
					Timber.d("Jellyseerr: API key stored (length: $apiKeyLength)")
					
					Toast.makeText(
						requireContext(),
						"Logged in successfully with permanent API key!",
						Toast.LENGTH_LONG
					).show()
					
					// Refresh the preferences screen to show updated API key status
					try {
						parentFragmentManager.beginTransaction()
							.detach(this@JellyseerrPreferencesScreen)
							.commitNow()
						parentFragmentManager.beginTransaction()
							.attach(this@JellyseerrPreferencesScreen)
							.commitNow()
					} catch (e: Exception) {
						Timber.w(e, "Failed to refresh preferences screen")
					}
				}.onFailure { error ->
					Timber.e(error, "Jellyseerr: Local login failed - ${error.message}")
					Toast.makeText(
						requireContext(),
						"Login failed: ${error.message}",
						Toast.LENGTH_LONG
					).show()
				}
			} catch (e: Exception) {
				Timber.e(e, "Jellyseerr: Local login exception")
				Toast.makeText(
					requireContext(),
					"Login error: ${e.message}",
					Toast.LENGTH_LONG
				).show()
			}
		}
	}

	private fun showServerUrlDialog() {
		val currentUrl = jellyseerrPreferences[JellyseerrPreferences.serverUrl] ?: ""
		val input = EditText(requireContext()).apply {
			setText(currentUrl)
			hint = "http://192.168.1.100:5055"
			inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
		}

		AlertDialog.Builder(requireContext())
			.setTitle(R.string.jellyseerr_server_url)
			.setMessage(R.string.jellyseerr_server_url_description)
			.setView(input)
			.setPositiveButton("Save") { _, _ ->
				val url = input.text.toString().trim()
				if (url.isNotEmpty()) {
					jellyseerrPreferences[JellyseerrPreferences.serverUrl] = url
					Toast.makeText(requireContext(), "Server URL saved", Toast.LENGTH_SHORT).show()
					Timber.d("Jellyseerr: Server URL saved: $url")
				}
			}
			.setNegativeButton("Cancel", null)
			.show()
	}

	private fun connectWithJellyfin() {
		val serverUrl = jellyseerrPreferences[JellyseerrPreferences.serverUrl]
		
		if (serverUrl.isNullOrBlank()) {
			Toast.makeText(requireContext(), "Please set server URL first", Toast.LENGTH_SHORT).show()
			return
		}

		// Get current Jellyfin user
		val currentUser = userRepository.currentUser.value
		val username = currentUser?.name
		if (username.isNullOrBlank()) {
			Toast.makeText(requireContext(), "Could not determine current user", Toast.LENGTH_SHORT).show()
			return
		}

		// Get Jellyfin server URL
		val jellyfinServerUrl = apiClient.baseUrl
		if (jellyfinServerUrl.isNullOrBlank()) {
			Toast.makeText(requireContext(), "Could not determine Jellyfin server URL", Toast.LENGTH_SHORT).show()
			return
		}

		// Show password dialog
		val passwordInput = EditText(requireContext()).apply {
			hint = "Enter your Jellyfin password"
			inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
		}

		val layout = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(48, 32, 48, 32)
			addView(TextView(requireContext()).apply {
				text = "Connecting as: $username\n\nEnter your Jellyfin password to authenticate with Jellyseerr"
				setPadding(0, 0, 0, 32)
			})
			addView(passwordInput)
		}

		AlertDialog.Builder(requireContext())
			.setTitle(R.string.jellyseerr_connect_jellyfin)
			.setView(layout)
			.setPositiveButton("Connect") { _, _ ->
				val password = passwordInput.text.toString()
				// Allow empty password for users without password
				performJellyfinLogin(serverUrl, username, password, jellyfinServerUrl)
			}
			.setNegativeButton("Cancel", null)
			.show()
	}

	private fun performJellyfinLogin(
		jellyseerrServerUrl: String,
		username: String,
		password: String,
		jellyfinServerUrl: String
	) {
		Toast.makeText(requireContext(), "Connecting...", Toast.LENGTH_SHORT).show()

		lifecycleScope.launch {
			try {
				// Get current Jellyfin user ID and switch cookie storage
				val currentUser = userRepository.currentUser.value
				val userId = currentUser?.id?.toString()
				if (userId != null) {
					JellyseerrHttpClient.switchCookieStorage(userId)
				}
				
				// Store current Jellyfin username
				jellyseerrPreferences[JellyseerrPreferences.lastJellyfinUser] = username
				
				val result = jellyseerrRepository.loginWithJellyfin(username, password, jellyfinServerUrl, jellyseerrServerUrl)
				
				result.onSuccess { user ->
					// Get API key if available, otherwise use empty string for cookie auth
					val apiKey = user.apiKey ?: ""

					// Save credentials
					jellyseerrPreferences[JellyseerrPreferences.serverUrl] = jellyseerrServerUrl
					jellyseerrPreferences[JellyseerrPreferences.enabled] = true
					jellyseerrPreferences[JellyseerrPreferences.lastConnectionSuccess] = true
					
					// Save API key if available
					if (apiKey.isNotEmpty()) {
						jellyseerrPreferences[JellyseerrPreferences.apiKey] = apiKey
					}
					
					// Initialize connection
					jellyseerrRepository.initialize(jellyseerrServerUrl, apiKey)
					
					val authType = if (apiKey.isEmpty()) {
						"session cookie (persists across restarts, ~30 day expiration)"
					} else {
						"API key (permanent)"
					}
					
					Toast.makeText(
						requireContext(),
						"Connected successfully using $authType!",
						Toast.LENGTH_LONG
					).show()
					
					Timber.d("Jellyseerr: Jellyfin authentication successful (using ${if (apiKey.isEmpty()) "cookie" else "API key"} authentication)")
					
					// Refresh the preferences screen to show updated API key status
					try {
						parentFragmentManager.beginTransaction()
							.detach(this@JellyseerrPreferencesScreen)
							.commitNow()
						parentFragmentManager.beginTransaction()
							.attach(this@JellyseerrPreferencesScreen)
							.commitNow()
					} catch (e: Exception) {
						Timber.w(e, "Failed to refresh preferences screen")
					}
				}.onFailure { error ->
					Toast.makeText(
						requireContext(),
						"Connection failed: ${error.message}",
						Toast.LENGTH_LONG
					).show()
					Timber.e(error, "Jellyseerr: Jellyfin authentication failed")
				}
			} catch (e: Exception) {
				Toast.makeText(
					requireContext(),
					"Connection error: ${e.message}",
					Toast.LENGTH_LONG
				).show()
				Timber.e(e, "Jellyseerr: Connection failed")
			}
		}
	}

	private fun testConnection() {
		val serverUrl = jellyseerrPreferences[JellyseerrPreferences.serverUrl]
		
		if (serverUrl.isNullOrBlank()) {
			Toast.makeText(requireContext(), "Please set server URL first", Toast.LENGTH_SHORT).show()
			return
		}

		Toast.makeText(requireContext(), "Testing connection...", Toast.LENGTH_SHORT).show()

		lifecycleScope.launch {
			try {
				// Initialize with empty API key (uses cookie auth)
				val result = jellyseerrRepository.initialize(serverUrl, "")
				
				result.onSuccess {
					Toast.makeText(
						requireContext(),
						R.string.jellyseerr_connection_success,
						Toast.LENGTH_SHORT
					).show()
					Timber.d("Jellyseerr: Connection test successful")
				}.onFailure { error ->
					Toast.makeText(
						requireContext(),
						"Connection failed: ${error.message}",
						Toast.LENGTH_LONG
					).show()
					Timber.e(error, "Jellyseerr: Connection test failed")
				}
			} catch (e: Exception) {
				Toast.makeText(
					requireContext(),
					"Connection error: ${e.message}",
					Toast.LENGTH_LONG
				).show()
				Timber.e(e, "Jellyseerr: Test connection failed")
			}
		}
	}

	private fun showProfileSelectionDialog(
		isMovie: Boolean,
		is4k: Boolean,
		currentProfileId: String?,
		onProfileSelected: (String) -> Unit
	) {
		val serverUrl = jellyseerrPreferences[JellyseerrPreferences.serverUrl]
		if (serverUrl.isNullOrBlank()) {
			Toast.makeText(requireContext(), "Please configure server URL first", Toast.LENGTH_SHORT).show()
			return
		}

		val loadingDialog = AlertDialog.Builder(requireContext())
			.setTitle(R.string.jellyseerr_profile_loading)
			.setMessage("Fetching available profiles from Jellyseerr...")
			.setCancelable(false)
			.create()

		loadingDialog.show()

		lifecycleScope.launch {
			try {
				// Fetch profiles from Jellyseerr
				val profiles = if (isMovie) {
					// Get Radarr settings
					val radarrSettingsResult = jellyseerrRepository.getRadarrSettings()
					radarrSettingsResult.getOrNull()?.let { radarrList ->
						// Find the appropriate server (4K or HD)
						val server = radarrList.find { it.is4k == is4k } ?: radarrList.firstOrNull()
						server?.profiles ?: emptyList()
					} ?: emptyList()
				} else {
					// Get Sonarr settings
					val sonarrSettingsResult = jellyseerrRepository.getSonarrSettings()
					sonarrSettingsResult.getOrNull()?.let { sonarrList ->
						// Find the appropriate server (4K or HD)
						val server = sonarrList.find { it.is4k == is4k } ?: sonarrList.firstOrNull()
						server?.profiles ?: emptyList()
					} ?: emptyList()
				}

				loadingDialog.dismiss()

				if (profiles.isEmpty()) {
					Toast.makeText(
						requireContext(),
						getString(R.string.jellyseerr_profile_error),
						Toast.LENGTH_LONG
					).show()
					return@launch
				}

				// Build profile selection dialog
				showProfileDialog(profiles, currentProfileId, onProfileSelected)

			} catch (e: Exception) {
				loadingDialog.dismiss()
				Timber.e(e, "Failed to fetch profiles")
				
				// Check if it's a permission error (403)
				val errorMessage = e.message ?: ""
				if (errorMessage.contains("403") || errorMessage.contains("permission", ignoreCase = true)) {
					Toast.makeText(
						requireContext(),
						getString(R.string.jellyseerr_profile_admin_required),
						Toast.LENGTH_LONG
					).show()
				} else {
					Toast.makeText(
						requireContext(),
						"${getString(R.string.jellyseerr_profile_error)} ${e.message}",
						Toast.LENGTH_LONG
					).show()
				}
			}
		}
	}

	private fun showProfileDialog(
		profiles: List<JellyseerrQualityProfileDto>,
		currentProfileId: String?,
		onProfileSelected: (String) -> Unit
	) {
		// Create options list with "Server Default" at the top
		val options = mutableListOf<Pair<String, String>>()
		options.add("" to getString(R.string.jellyseerr_profile_default))
		profiles.forEach { profile ->
			options.add(profile.id.toString() to profile.name)
		}

		// Find currently selected index
		val currentIndex = if (currentProfileId.isNullOrEmpty()) {
			0 // Server Default
		} else {
			options.indexOfFirst { it.first == currentProfileId }.takeIf { it >= 0 } ?: 0
		}

		val optionNames = options.map { it.second }.toTypedArray()

		AlertDialog.Builder(requireContext())
			.setTitle("Select Quality Profile")
			.setSingleChoiceItems(optionNames, currentIndex) { dialog, which ->
				val selectedProfileId = options[which].first
				onProfileSelected(selectedProfileId)
				
				Toast.makeText(
					requireContext(),
					"Profile set to: ${options[which].second}",
					Toast.LENGTH_SHORT
				).show()
				
				dialog.dismiss()

				// Refresh the preferences screen to show updated selection
				try {
					parentFragmentManager.beginTransaction()
						.detach(this@JellyseerrPreferencesScreen)
						.commitNow()
					parentFragmentManager.beginTransaction()
						.attach(this@JellyseerrPreferencesScreen)
						.commitNow()
				} catch (e: Exception) {
					Timber.w(e, "Failed to refresh preferences screen")
				}
			}
			.setNegativeButton("Cancel", null)
			.show()
	}
}
