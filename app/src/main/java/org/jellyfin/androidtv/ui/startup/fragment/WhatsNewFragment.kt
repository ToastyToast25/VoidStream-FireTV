package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme

class WhatsNewFragment : Fragment() {
	companion object {
		private const val ARG_VERSION = "arg_ver"
		private const val ARG_NOTES = "arg_notes"

		fun newInstance(ver: String, notes: String) = WhatsNewFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_VERSION, ver)
				putString(ARG_NOTES, notes)
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val ver = arguments?.getString(ARG_VERSION) ?: ""
		val notes = arguments?.getString(ARG_NOTES) ?: ""
		JellyfinTheme {
			PostUpdateScreen(ver, notes) {
				parentFragmentManager.setFragmentResult("whats_new_done", Bundle.EMPTY)
			}
		}
	}
}

@Composable
private fun PostUpdateScreen(
	displayVersion: String,
	changelogText: String,
	onContinueClicked: () -> Unit,
) {
	val noteScrollState = rememberScrollState()
	val coroutines = rememberCoroutineScope()
	val btnFocus = remember { FocusRequester() }

	LaunchedEffect(Unit) { btnFocus.requestFocus() }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colorResource(R.color.not_quite_black))
			.focusable(),
		contentAlignment = Alignment.Center,
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			// App branding
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = null,
				modifier = Modifier.height(72.dp).padding(bottom = 20.dp),
			)

			// Main card container
			WhatsNewCard(
				displayVersion = displayVersion,
				changelogText = changelogText,
				noteScrollState = noteScrollState,
				coroutines = coroutines,
				btnFocus = btnFocus,
				onContinueClicked = onContinueClicked,
			)
		}
	}
}

@Composable
private fun WhatsNewCard(
	displayVersion: String,
	changelogText: String,
	noteScrollState: androidx.compose.foundation.ScrollState,
	coroutines: kotlinx.coroutines.CoroutineScope,
	btnFocus: FocusRequester,
	onContinueClicked: () -> Unit,
) {
	val cardShape = RoundedCornerShape(16.dp)
	val borderTint = Color(0xFF3A1A1A)
	val cardBg = Color(0xFF1A1212)
	val headerBg = Color(0xFF201414)
	val accentRed = Color(0xFFCC3333)

	Column(
		modifier = Modifier
			.width(480.dp)
			.clip(cardShape)
			.background(cardBg)
			.border(1.dp, borderTint, cardShape),
	) {
		// -- Card header --
		CardHeader(displayVersion, headerBg, accentRed)

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Scrollable changelog body --
		Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(noteScrollState)
					.padding(28.dp),
			) {
				MarkdownBody(changelogText, accentRed)
				Spacer(modifier = Modifier.height(8.dp))
			}

			// Fade gradient at bottom edge
			Box(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
					.height(32.dp)
					.background(Brush.verticalGradient(listOf(Color.Transparent, cardBg))),
			)
		}

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Footer with continue button --
		CardFooter(
			accentRed = accentRed,
			headerBg = headerBg,
			btnFocus = btnFocus,
			noteScrollState = noteScrollState,
			coroutines = coroutines,
			onContinueClicked = onContinueClicked,
		)
	}
}

@Composable
private fun CardHeader(displayVersion: String, headerBg: Color, accentRed: Color) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(headerBg)
			.padding(horizontal = 28.dp, vertical = 20.dp),
	) {
		Text(
			text = stringResource(R.string.whats_new_title),
			color = Color.White,
			fontSize = 20.sp,
			fontWeight = FontWeight.Bold,
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = stringResource(R.string.whats_new_updated_to, displayVersion),
			color = accentRed,
			fontSize = 14.sp,
			fontWeight = FontWeight.Medium,
		)
	}
}

@Composable
private fun MarkdownBody(rawText: String, bulletColor: Color) {
	val textColor = Color(0xFFCCCCCC)
	for (line in rawText.lines()) {
		when {
			line.startsWith("### ") -> {
				Spacer(modifier = Modifier.height(16.dp))
				Text(line.removePrefix("### "), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
				Spacer(modifier = Modifier.height(8.dp))
			}
			line.startsWith("## ") -> {
				Spacer(modifier = Modifier.height(16.dp))
				Text(line.removePrefix("## "), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(8.dp))
			}
			line.startsWith("# ") -> {
				Spacer(modifier = Modifier.height(16.dp))
				Text(line.removePrefix("# "), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(8.dp))
			}
			line.startsWith("- ") || line.startsWith("* ") -> {
				Row(modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)) {
					Text("\u2022  ", color = bulletColor, fontSize = 15.sp)
					Text(
						line.removePrefix("- ").removePrefix("* "),
						color = textColor, fontSize = 15.sp, lineHeight = 21.sp,
					)
				}
			}
			line.isBlank() -> Spacer(modifier = Modifier.height(8.dp))
			else -> Text(line, color = textColor, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(bottom = 4.dp))
		}
	}
}

@Composable
private fun CardFooter(
	accentRed: Color,
	headerBg: Color,
	btnFocus: FocusRequester,
	noteScrollState: androidx.compose.foundation.ScrollState,
	coroutines: kotlinx.coroutines.CoroutineScope,
	onContinueClicked: () -> Unit,
) {
	var hasFocus by remember { mutableStateOf(false) }
	val focusedBg = Color(0xFFE04444)

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.background(headerBg)
			.padding(horizontal = 28.dp, vertical = 16.dp),
		contentAlignment = Alignment.CenterEnd,
	) {
		Button(
			onClick = onContinueClicked,
			colors = ButtonDefaults.buttonColors(
				containerColor = if (hasFocus) focusedBg else accentRed,
				contentColor = Color.White,
			),
			shape = RoundedCornerShape(8.dp),
			modifier = Modifier
				.width(140.dp)
				.height(40.dp)
				.focusRequester(btnFocus)
				.onPreviewKeyEvent { ev ->
					if (ev.type == KeyEventType.KeyDown) {
						when (ev.key) {
							Key.DirectionDown -> {
								coroutines.launch { noteScrollState.animateScrollBy(200f) }
								true
							}
							Key.DirectionUp -> {
								coroutines.launch { noteScrollState.animateScrollBy(-200f) }
								true
							}
							else -> false
						}
					} else false
				}
				.then(
					if (hasFocus) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
					else Modifier
				)
				.onFocusChanged { hasFocus = it.isFocused },
		) {
			Text(
				text = stringResource(R.string.whats_new_continue),
				fontSize = 14.sp,
				fontWeight = FontWeight.Medium,
			)
		}
	}
}
