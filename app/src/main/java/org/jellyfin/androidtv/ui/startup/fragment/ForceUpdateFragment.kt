package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.koin.android.ext.android.inject
import timber.log.Timber

class ForceUpdateFragment : Fragment() {
	companion object {
		const val ARG_VERSION = "version"
		const val ARG_DOWNLOAD_URL = "download_url"
		const val ARG_APK_SIZE = "apk_size"
		const val ARG_RELEASE_NOTES = "release_notes"
		const val ARG_RELEASE_URL = "release_url"

		fun newInstance(updateInfo: UpdateCheckerService.UpdateInfo) = ForceUpdateFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_VERSION, updateInfo.version)
				putString(ARG_DOWNLOAD_URL, updateInfo.downloadUrl)
				putLong(ARG_APK_SIZE, updateInfo.apkSize)
				putString(ARG_RELEASE_NOTES, updateInfo.releaseNotes)
				putString(ARG_RELEASE_URL, updateInfo.releaseUrl)
			}
		}
	}

	private val updateCheckerService: UpdateCheckerService by inject()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			ForceUpdateScreen(
				version = arguments?.getString(ARG_VERSION) ?: "",
				downloadUrl = arguments?.getString(ARG_DOWNLOAD_URL) ?: "",
				apkSize = arguments?.getLong(ARG_APK_SIZE) ?: 0L,
				releaseNotes = arguments?.getString(ARG_RELEASE_NOTES) ?: "",
				onDownloadAndInstall = { onProgress ->
					downloadAndInstall(
						arguments?.getString(ARG_DOWNLOAD_URL) ?: "",
						onProgress
					)
				}
			)
		}
	}

	private suspend fun downloadAndInstall(
		downloadUrl: String,
		onProgress: (Float) -> Unit
	): Boolean {
		// Check install permission on Android 8+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (!requireContext().packageManager.canRequestPackageInstalls()) {
				withContext(Dispatchers.Main) {
					Toast.makeText(requireContext(), "Please grant install permission", Toast.LENGTH_LONG).show()
					val intent = Intent(
						Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
						android.net.Uri.parse("package:${requireContext().packageName}")
					)
					startActivity(intent)
				}
				return false
			}
		}

		return try {
			val result = updateCheckerService.downloadUpdate(downloadUrl) { progress ->
				onProgress(progress / 100f)
			}
			result.fold(
				onSuccess = { apkUri ->
					updateCheckerService.installUpdate(apkUri)
					true
				},
				onFailure = { error ->
					Timber.e(error, "Failed to download update")
					false
				}
			)
		} catch (e: Exception) {
			Timber.e(e, "Error downloading update")
			false
		}
	}
}

private enum class UpdateState {
	READY,
	DOWNLOADING,
	INSTALLING,
	FAILED
}

@Composable
private fun ForceUpdateScreen(
	version: String,
	downloadUrl: String,
	apkSize: Long,
	releaseNotes: String,
	onDownloadAndInstall: suspend ((Float) -> Unit) -> Boolean,
) {
	var state by remember { mutableStateOf(UpdateState.READY) }
	var progress by remember { mutableFloatStateOf(0f) }
	val scope = rememberCoroutineScope()

	val sizeMB = String.format("%.1f", apkSize / (1024.0 * 1024.0))

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colorResource(id = R.color.not_quite_black)),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.fillMaxSize(),
		) {
			// Logo
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = stringResource(R.string.app_name),
				modifier = Modifier
					.width(200.dp)
					.height(120.dp)
			)

			Spacer(modifier = Modifier.height(32.dp))

			// Update card
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier
					.width(480.dp)
					.background(
						color = Color(0xFF1A1A2E),
						shape = RoundedCornerShape(16.dp)
					)
					.border(
						width = 1.dp,
						color = Color(0xFF2A2A4A),
						shape = RoundedCornerShape(16.dp)
					)
					.padding(32.dp)
			) {
				// Title
				Text(
					text = stringResource(R.string.force_update_title),
					color = Color.White,
					fontSize = 24.sp,
					fontWeight = FontWeight.Bold,
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Message
				Text(
					text = stringResource(R.string.force_update_message, version),
					color = Color(0xFFB0B0B0),
					fontSize = 16.sp,
					textAlign = TextAlign.Center,
					lineHeight = 22.sp,
				)

				Spacer(modifier = Modifier.height(12.dp))

				// Size
				Text(
					text = stringResource(R.string.force_update_size, sizeMB),
					color = Color(0xFF808080),
					fontSize = 14.sp,
				)

				Spacer(modifier = Modifier.height(24.dp))

				// Progress bar (visible during download)
				if (state == UpdateState.DOWNLOADING) {
					Text(
						text = stringResource(R.string.force_update_downloading),
						color = Color(0xFFB0B0B0),
						fontSize = 14.sp,
					)
					Spacer(modifier = Modifier.height(8.dp))
					LinearProgressIndicator(
						progress = { progress },
						modifier = Modifier
							.fillMaxWidth()
							.height(8.dp),
						color = Color(0xFFAA5CC3),
						trackColor = Color(0xFF2A2A4A),
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = "${(progress * 100).toInt()}%",
						color = Color(0xFF808080),
						fontSize = 12.sp,
					)
					Spacer(modifier = Modifier.height(16.dp))
				}

				if (state == UpdateState.INSTALLING) {
					Text(
						text = stringResource(R.string.force_update_installing),
						color = Color(0xFFAA5CC3),
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
					Spacer(modifier = Modifier.height(16.dp))
				}

				// Update Now button
				var updateButtonFocused by remember { mutableStateOf(false) }
				Button(
					onClick = {
						if (state == UpdateState.READY || state == UpdateState.FAILED) {
							state = UpdateState.DOWNLOADING
							progress = 0f
							scope.launch(Dispatchers.IO) {
								val success = onDownloadAndInstall { p ->
									progress = p
								}
								withContext(Dispatchers.Main) {
									state = if (success) UpdateState.INSTALLING else UpdateState.FAILED
								}
							}
						}
					},
					enabled = state == UpdateState.READY || state == UpdateState.FAILED,
					colors = ButtonDefaults.buttonColors(
						containerColor = if (updateButtonFocused) Color(0xFFBB6DD4) else Color(0xFFAA5CC3),
						contentColor = Color.White,
						disabledContainerColor = Color(0xFF3A3A5A),
						disabledContentColor = Color(0xFF808080),
					),
					shape = RoundedCornerShape(8.dp),
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp)
						.onFocusChanged { updateButtonFocused = it.isFocused }
						.focusable(),
				) {
					Text(
						text = when (state) {
							UpdateState.READY -> stringResource(R.string.force_update_button)
							UpdateState.DOWNLOADING -> stringResource(R.string.force_update_downloading)
							UpdateState.INSTALLING -> stringResource(R.string.force_update_installing)
							UpdateState.FAILED -> stringResource(R.string.force_update_download_failed)
						},
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
				}

				// Release Notes button (only when not downloading)
				if (state == UpdateState.READY || state == UpdateState.FAILED) {
					Spacer(modifier = Modifier.height(8.dp))
					var notesButtonFocused by remember { mutableStateOf(false) }
					TextButton(
						onClick = { /* Release notes handled by parent activity dialog */ },
						colors = ButtonDefaults.textButtonColors(
							contentColor = if (notesButtonFocused) Color(0xFFBB6DD4) else Color(0xFFAA5CC3),
						),
						modifier = Modifier
							.fillMaxWidth()
							.height(40.dp)
							.onFocusChanged { notesButtonFocused = it.isFocused }
							.focusable(),
					) {
						Text(
							text = stringResource(R.string.force_update_release_notes),
							fontSize = 14.sp,
						)
					}
				}
			}
		}
	}
}
