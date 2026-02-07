package org.jellyfin.androidtv.data.service

import android.content.Context
import android.os.Process
import android.util.Log
import org.jellyfin.androidtv.util.appendCodeBlock
import org.jellyfin.androidtv.util.appendDetails
import org.jellyfin.androidtv.util.buildMarkdown
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AppLogCollector private constructor() {

	companion object {
		private const val RING_BUFFER_CAPACITY = 200
		private const val CRASH_LOG_FILENAME = "last_crash.log"
		private const val MAX_LOG_SECTION_CHARS = 50_000
		private const val MAX_CRASH_CHARS = 10_000
		private const val MAX_BUFFER_CHARS = 30_000
		private const val MAX_LOGCAT_CHARS = 15_000
		private const val LOGCAT_LINES = 300
		private const val CRASH_CONTEXT_LINES = 50

		@Volatile
		private var _instance: AppLogCollector? = null

		val instance: AppLogCollector
			get() = _instance ?: synchronized(this) {
				_instance ?: AppLogCollector().also { _instance = it }
			}
	}

	// Ring buffer
	private val buffer = arrayOfNulls<String>(RING_BUFFER_CAPACITY)
	private var head = 0
	private var count = 0
	private val lock = ReentrantReadWriteLock()

	// Crash file directory
	private var crashLogDir: File? = null

	// Custom Timber tree that captures log lines into the ring buffer
	val tree: Timber.Tree = object : Timber.DebugTree() {
		private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
			override fun initialValue() = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
		}

		override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
			val level = when (priority) {
				Log.VERBOSE -> "V"
				Log.DEBUG -> "D"
				Log.INFO -> "I"
				Log.WARN -> "W"
				Log.ERROR -> "E"
				Log.ASSERT -> "A"
				else -> "?"
			}
			val timestamp = dateFormat.get()!!.format(Date())
			val line = "$timestamp $level/${tag ?: "---"}: $message"

			lock.write {
				buffer[head] = line
				head = (head + 1) % RING_BUFFER_CAPACITY
				if (count < RING_BUFFER_CAPACITY) count++
			}
		}
	}

	/**
	 * Initialize crash log persistence and install the crash handler.
	 * Must be called once with the Application context from LogInitializer.
	 */
	fun init(context: Context) {
		crashLogDir = context.filesDir
		installCrashHandler()
	}

	/**
	 * Returns the current ring buffer contents, oldest-first.
	 */
	fun getBufferedLogs(): List<String> {
		lock.read {
			if (count == 0) return emptyList()
			val result = ArrayList<String>(count)
			val start = if (count < RING_BUFFER_CAPACITY) 0 else head
			for (i in 0 until count) {
				val idx = (start + i) % RING_BUFFER_CAPACITY
				buffer[idx]?.let { result.add(it) }
			}
			return result
		}
	}

	/**
	 * Capture logcat output for this process only.
	 * Uses --pid flag (Android 7.0+) which works on Fire TV and Shield without root.
	 */
	fun captureLogcat(): String? {
		return try {
			val pid = Process.myPid()
			val process = Runtime.getRuntime().exec(
				arrayOf("logcat", "-d", "-v", "threadtime", "--pid=$pid", "-t", LOGCAT_LINES.toString())
			)
			val output = process.inputStream.bufferedReader().readText()
			process.waitFor()
			output.ifBlank { null }
		} catch (e: Exception) {
			Timber.w(e, "Failed to capture logcat")
			null
		}
	}

	/**
	 * Read the last crash stack trace from the previous session, then delete the file.
	 */
	fun getAndClearLastCrash(): String? {
		val file = crashLogDir?.resolve(CRASH_LOG_FILENAME) ?: return null
		return try {
			if (!file.exists()) return null
			val content = file.readText()
			file.delete()
			content.ifBlank { null }
		} catch (e: Exception) {
			Timber.w(e, "Failed to read crash log")
			null
		}
	}

	/**
	 * Build the full log section for the GitHub issue body.
	 * Returns a markdown string with collapsed <details> blocks, or empty string if no logs.
	 */
	fun buildLogSection(): String {
		val buffered = getBufferedLogs()
		val lastCrash = getAndClearLastCrash()
		val logcat = captureLogcat()

		if (buffered.isEmpty() && lastCrash == null && logcat == null) return ""

		val section = buildMarkdown {
			appendLine()
			appendLine("---")

			if (!lastCrash.isNullOrBlank()) {
				appendDetails("Last Crash (previous session)") {
					appendCodeBlock("", truncate(lastCrash, MAX_CRASH_CHARS))
				}
			}

			if (buffered.isNotEmpty()) {
				appendDetails("App Logs (last ${buffered.size} entries)") {
					val logsText = buffered.joinToString("\n")
					appendCodeBlock("", truncate(logsText, MAX_BUFFER_CHARS))
				}
			}

			if (!logcat.isNullOrBlank()) {
				appendDetails("Logcat") {
					appendCodeBlock("", truncate(logcat, MAX_LOGCAT_CHARS))
				}
			}
		}

		return if (section.length > MAX_LOG_SECTION_CHARS) {
			section.take(MAX_LOG_SECTION_CHARS - 20) + "\n... (truncated)"
		} else {
			section
		}
	}

	private fun truncate(text: String, maxLen: Int): String {
		if (text.length <= maxLen) return text
		return text.take(maxLen) + "\n... (truncated)"
	}

	private fun installCrashHandler() {
		val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			writeCrashToFile(throwable)
			existingHandler?.uncaughtException(thread, throwable)
		}
	}

	private fun writeCrashToFile(throwable: Throwable) {
		try {
			val file = crashLogDir?.resolve(CRASH_LOG_FILENAME) ?: return
			val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
			val content = buildString {
				appendLine("Crash at: ${dateFormat.format(Date())}")
				appendLine()
				appendLine(throwable.stackTraceToString())
				appendLine()
				appendLine("--- Recent logs before crash ---")
				val recentLogs = getBufferedLogs().takeLast(CRASH_CONTEXT_LINES)
				recentLogs.forEach { appendLine(it) }
			}
			file.writeText(content)
		} catch (_: Exception) {
			// Best-effort: never throw from crash handler
		}
	}
}
