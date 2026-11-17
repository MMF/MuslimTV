package com.mfouad.muslimtv.core.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.mfouad.muslimtv.core.designsystem.component.IconButton
import com.mfouad.muslimtv.core.designsystem.component.FocusCard
import com.mfouad.muslimtv.core.designsystem.focus.requestFocusSafely
import com.mfouad.muslimtv.core.designsystem.theme.MtvShapes
import com.mfouad.muslimtv.core.designsystem.theme.MtvSpacing
import com.mfouad.muslimtv.core.designsystem.theme.MtvTheme

/**
 * Remote-friendly search. Moving focus over the field does not pop up the keyboard; OK starts
 * typing, and Back or the keyboard's Search key finishes. A voice button sits beside it when the
 * TV has speech recognition.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    fieldRequester: FocusRequester = remember { FocusRequester() },
    onDone: () -> Unit = {},
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val input = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val c = MtvTheme.colors

    fun finish() {
        editing = false
        keyboard?.hide()
        onDone()
    }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MtvSpacing.m), verticalAlignment = Alignment.CenterVertically) {
        FocusCard(
            onClick = { editing = true },
            modifier = Modifier.weight(1f).focusRequester(fieldRequester),
            focusedScale = 1.02f,
            shape = MtvShapes.button,
            contentPadding = 0.dp,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(if (editing) Modifier.border(BorderStroke(2.5.dp, c.gold), MtvShapes.button) else Modifier)
                    .padding(horizontal = MtvSpacing.m, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MtvSpacing.s),
            ) {
                Icon(Icons.Rounded.Search, null, Modifier.size(30.dp), tint = c.gold)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(hint, style = MtvTheme.type.body, color = c.onBackgroundFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MtvTheme.type.body.copy(color = c.onBackground),
                        cursorBrush = SolidColor(c.gold),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { finish() }, onDone = { finish() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(input)
                            .focusProperties { canFocus = editing }
                            .onPreviewKeyEvent { e ->
                                if (editing && e.type == KeyEventType.KeyUp && (e.key == Key.Back || e.key == Key.DirectionDown)) {
                                    finish()
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                }
            }
        }
        VoiceSearchButton(onResult = { onQueryChange(it); onDone() })
    }

    // Only react to a change of editing state, never on first composition (that would steal focus).
    var wasEditing by remember { mutableStateOf(editing) }
    LaunchedEffect(editing) {
        if (editing) {
            input.requestFocusSafely()
            keyboard?.show()
        } else if (wasEditing) {
            fieldRequester.requestFocusSafely(maxFrames = 2)
        }
        wasEditing = editing
    }
}

@Composable
private fun VoiceSearchButton(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    }
    val available = remember { intent.resolveActivity(context.packageManager) != null }
    if (!available) return
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onResult)
        }
    }
    IconButton(
        icon = Icons.Rounded.Mic,
        contentDescription = stringResource(R.string.action_voice_search),
        onClick = { runCatching { launcher.launch(intent) } },
    )
}
