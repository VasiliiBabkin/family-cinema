package ru.familycinema.app.library

import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.familycinema.app.R

/** TV uses the platform editor/IME bridge; other form factors retain Material Compose input. */
@Composable
internal fun SourceUrlField(
    value: String,
    change: (String) -> Unit,
    label: String,
    tv: Boolean,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    if (!tv) {
        val hidden = remember { PasswordVisualTransformation() }
        OutlinedTextField(
            value,
            { change(it.take(8192)) },
            modifier,
            label = { Text(label) },
            singleLine = true,
            visualTransformation = hidden,
            keyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        focus.clearFocus()
                    }
                ),
        )
        return
    }
    val latestChange by rememberUpdatedState(change)
    val latestValue by rememberUpdatedState(value)
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        AndroidView(
            modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
            factory = { context ->
                val density = context.resources.displayMetrics.density
                EditText(context).apply {
                    tag = "source_endpoint_native"
                    contentDescription = label
                    setSingleLine(true)
                    textSize = 20f
                    setTextColor(colors.onSurface.toArgb())
                    setHintTextColor(colors.onSurfaceVariant.toArgb())
                    hint = context.getString(R.string.native_url_hint)
                    inputType =
                        InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD or
                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    transformationMethod = PasswordTransformationMethod.getInstance()
                    filters = arrayOf(InputFilter.LengthFilter(8192))
                    imeOptions =
                        EditorInfo.IME_ACTION_DONE or
                            EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    setPadding(
                        (16 * density).toInt(),
                        (12 * density).toInt(),
                        (16 * density).toInt(),
                        (12 * density).toInt(),
                    )
                    val outline =
                        GradientDrawable().apply {
                            cornerRadius = 12 * density
                            setColor(colors.surface.toArgb())
                            setStroke((2 * density).toInt(), colors.outline.toArgb())
                        }
                    background = outline
                    onFocusChangeListener =
                        android.view.View.OnFocusChangeListener { _, hasFocus ->
                            outline.setStroke(
                                ((if (hasFocus) 3 else 2) * density).toInt(),
                                (if (hasFocus) colors.primary else colors.outline).toArgb(),
                            )
                        }
                    addTextChangedListener(
                        object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence?,
                                start: Int,
                                count: Int,
                                after: Int,
                            ) {}

                            override fun onTextChanged(
                                s: CharSequence?,
                                start: Int,
                                before: Int,
                                count: Int,
                            ) {
                                val text = s?.toString().orEmpty()
                                if (text != latestValue) latestChange(text)
                            }

                            override fun afterTextChanged(s: Editable?) {}
                        }
                    )
                    setOnEditorActionListener { view, action, _ ->
                        if (action == EditorInfo.IME_ACTION_DONE) {
                            (context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                                    as InputMethodManager)
                                .hideSoftInputFromWindow(view.windowToken, 0)
                            clearFocus()
                            focus.clearFocus()
                            true
                        } else false
                    }
                }
            },
            update = { view ->
                if (view.text.toString() != value) {
                    view.setText(value)
                    view.setSelection(value.length)
                }
            },
            onRelease = { view ->
                view.clearFocus()
                view.setOnEditorActionListener(null)
                view.onFocusChangeListener = null
            },
        )
    }
}
