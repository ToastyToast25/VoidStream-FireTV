package org.jellyfin.androidtv.ui.jellyseerr

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import org.jellyfin.androidtv.R

/**
 * Dialog for selecting request quality (HD or 4K)
 */
class QualitySelectionDialog(
	context: Context,
	private val title: String,
	private val canRequestHd: Boolean,
	private val canRequest4k: Boolean,
	private val hdStatus: Int?, // Current HD status (null, 1-6)
	private val status4k: Int?, // Current 4K status (null, 1-6)
	private val onSelect: (is4k: Boolean) -> Unit
) : Dialog(context) {

	private lateinit var hdButton: TextView
	private lateinit var fourKButton: TextView
	private lateinit var cancelButton: TextView
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
		val rootContainer = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			setBackgroundColor(Color.parseColor("#1F2937"))
			setPadding(32.dp(context), 24.dp(context), 32.dp(context), 24.dp(context))
			layoutParams = ViewGroup.LayoutParams(
				500.dp(context),
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
		}
		
		val titleText = TextView(context).apply {
			text = context.getString(R.string.quality_select_title)
			textSize = 20f
			setTextColor(Color.WHITE)
			setTypeface(typeface, android.graphics.Typeface.BOLD)
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = 8.dp(context)
			}
		}
		rootContainer.addView(titleText)
		
		val subtitleText = TextView(context).apply {
			text = title
			textSize = 14f
			setTextColor(Color.parseColor("#9CA3AF"))
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = 24.dp(context)
			}
		}
		rootContainer.addView(subtitleText)
		
		val buttonsContainer = LinearLayout(context).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}
		
		val hdLabel = getQualityLabel(false, hdStatus)
		val isHdDisabled = !canRequestHd || isStatusBlocked(hdStatus)
		
		hdButton = createButton(
			text = hdLabel,
			isEnabled = !isHdDisabled,
			isPrimary = true
		) {
			onSelect(false)
			dismiss()
		}
		buttonsContainer.addView(hdButton)
		
		val fourKLabel = getQualityLabel(true, status4k)
		val is4kDisabled = !canRequest4k || isStatusBlocked(status4k)
		
		fourKButton = createButton(
			text = fourKLabel,
			isEnabled = !is4kDisabled,
			isPrimary = true
		) {
			onSelect(true)
			dismiss()
		}
		(fourKButton.layoutParams as LinearLayout.LayoutParams).topMargin = 12.dp(context)
		buttonsContainer.addView(fourKButton)
		
		rootContainer.addView(buttonsContainer)
		
		cancelButton = createButton(
			text = context.getString(R.string.lbl_cancel),
			isEnabled = true,
			isPrimary = false
		) {
			dismiss()
		}
		(cancelButton.layoutParams as LinearLayout.LayoutParams).topMargin = 24.dp(context)
		rootContainer.addView(cancelButton)
		
		setContentView(rootContainer)
		
		window?.apply {
			setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
			setLayout(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)
			setGravity(Gravity.CENTER)
		}
		
		val firstEnabled = when {
			!isHdDisabled -> hdButton
			!is4kDisabled -> fourKButton
			else -> cancelButton
		}
		firstEnabled.requestFocus()
		
		setupFocusNavigation()
	}
	
	private fun getQualityLabel(is4k: Boolean, status: Int?): String {
		val qualityPrefix = if (is4k) "4K" else "HD"
		return when (status) {
			2 -> context.getString(R.string.quality_status_pending, qualityPrefix)
			3 -> context.getString(R.string.quality_status_processing, qualityPrefix)
			4 -> context.getString(R.string.quality_request_more, qualityPrefix)
			5 -> context.getString(R.string.quality_status_available, qualityPrefix)
			6 -> context.getString(R.string.quality_status_blacklisted, qualityPrefix)
			else -> context.getString(R.string.quality_request, qualityPrefix)
		}
	}
	
	private fun isStatusBlocked(status: Int?): Boolean {
		return status != null && status >= 2 && status != 4
	}
	
	private fun createButton(
		text: String,
		isEnabled: Boolean,
		isPrimary: Boolean,
		onClick: () -> Unit
	): TextView {
		return TextView(context).apply {
			this.text = text
			textSize = 16f
			setTextColor(if (isEnabled) Color.WHITE else Color.parseColor("#6B7280"))
			setTypeface(typeface, android.graphics.Typeface.BOLD)
			gravity = Gravity.CENTER
			setPadding(24.dp(context), 16.dp(context), 24.dp(context), 16.dp(context))
			
			background = android.graphics.drawable.GradientDrawable().apply {
				setColor(
					when {
						!isEnabled -> Color.parseColor("#374151")
						isPrimary -> Color.parseColor("#6366F1")
						else -> Color.parseColor("#4B5563")
					}
				)
				cornerRadius = 8.dp(context).toFloat()
			}
			
			alpha = if (isEnabled) 1f else 0.5f
			isFocusable = isEnabled
			isFocusableInTouchMode = isEnabled
			this.isEnabled = isEnabled
			
			if (isEnabled) {
				setOnClickListener { onClick() }
				setOnFocusChangeListener { v, hasFocus ->
					(v.background as? android.graphics.drawable.GradientDrawable)?.setColor(
						when {
							hasFocus && isPrimary -> Color.parseColor("#818CF8")
							hasFocus && !isPrimary -> Color.parseColor("#6B7280")
							isPrimary -> Color.parseColor("#6366F1")
							else -> Color.parseColor("#4B5563")
						}
					)
				}
			}
			
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		}
	}
	
	private fun setupFocusNavigation() {
		val focusableButtons = listOfNotNull(
			hdButton.takeIf { it.isEnabled },
			fourKButton.takeIf { it.isEnabled },
			cancelButton
		)
		
		focusableButtons.forEachIndexed { index, button ->
			button.id = View.generateViewId()
		}
		
		focusableButtons.forEachIndexed { index, button ->
			val prevButton = focusableButtons.getOrNull(index - 1)
			val nextButton = focusableButtons.getOrNull(index + 1)
			
			button.nextFocusUpId = prevButton?.id ?: button.id
			button.nextFocusDownId = nextButton?.id ?: button.id
		}
	}
	
	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		when (keyCode) {
			KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
				dismiss()
				return true
			}
		}
		return super.onKeyDown(keyCode, event)
	}
	
	private fun Int.dp(context: Context): Int {
		return (this * context.resources.displayMetrics.density).toInt()
	}
}
