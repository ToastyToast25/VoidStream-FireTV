package org.jellyfin.androidtv

import android.content.Context
import androidx.startup.Initializer
import org.jellyfin.androidtv.data.service.AppLogCollector
import timber.log.Timber

class LogInitializer : Initializer<Unit> {
	override fun create(context: Context) {
		// Enable improved logging for leaking resources
		// https://wh0.github.io/2020/08/12/closeguard.html
		if (BuildConfig.DEBUG) {
			try {
				Class.forName("dalvik.system.CloseGuard")
					.getMethod("setEnabled", Boolean::class.javaPrimitiveType)
					.invoke(null, true)
			} catch (e: ReflectiveOperationException) {
				@Suppress("TooGenericExceptionThrown")
				throw RuntimeException(e)
			}
		}

		// Initialize the logging library
		Timber.plant(Timber.DebugTree())

		// Plant log collector tree for in-memory ring buffer capture
		val logCollector = AppLogCollector.instance
		Timber.plant(logCollector.tree)
		logCollector.init(context.applicationContext)

		Timber.i("Debug tree and log collector planted")
	}

	override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
