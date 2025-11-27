package org.jellyfin.androidtv.ui.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.ui.startup.StartupActivity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Background worker that checks for app updates
 */
class UpdateCheckWorker(
	context: Context,
	params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

	private val updateChecker by inject<UpdateCheckerService>()

	companion object {
		const val NOTIFICATION_CHANNEL_ID = "app_updates"
		const val NOTIFICATION_ID = 1001
		const val WORK_NAME = "update_check_work"
	}

	override suspend fun doWork(): Result {
		return try {
			Timber.d("Checking for app updates...")
			
			val result = updateChecker.checkForUpdate()
			result.fold(
				onSuccess = { updateInfo ->
					if (updateInfo != null && updateInfo.isNewer) {
						Timber.i("Update available: ${updateInfo.version}")
						showUpdateNotification(updateInfo)
					} else {
						Timber.d("No updates available")
					}
				},
				onFailure = { error ->
					Timber.e(error, "Failed to check for updates")
				}
			)

			Result.success()
		} catch (e: Exception) {
			Timber.e(e, "Update check worker failed")
			Result.failure()
		}
	}

	private fun showUpdateNotification(updateInfo: UpdateCheckerService.UpdateInfo) {
		val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

		// Create notification channel for Android O+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val channel = NotificationChannel(
				NOTIFICATION_CHANNEL_ID,
				applicationContext.getString(R.string.notification_channel_updates),
				NotificationManager.IMPORTANCE_DEFAULT
			).apply {
				description = applicationContext.getString(R.string.notification_channel_updates_description)
			}
			notificationManager.createNotificationChannel(channel)
		}

		// Create intent to open preferences
		val intent = Intent(applicationContext, StartupActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
			putExtra("open_updates", true)
		}

		val pendingIntent = PendingIntent.getActivity(
			applicationContext,
			0,
			intent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		// Build notification
		val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_jellyfin)
			.setContentTitle(applicationContext.getString(R.string.notification_update_available_title))
			.setContentText(applicationContext.getString(R.string.notification_update_available_text, updateInfo.version))
			.setStyle(NotificationCompat.BigTextStyle()
				.bigText(applicationContext.getString(R.string.notification_update_available_text, updateInfo.version)))
			.setPriority(NotificationCompat.PRIORITY_DEFAULT)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build()

		notificationManager.notify(NOTIFICATION_ID, notification)
	}
}
