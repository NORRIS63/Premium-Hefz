package app.premiumhefz.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.premiumhefz.R
import app.premiumhefz.domain.PageCoordinates
import app.premiumhefz.ui.LocalContainer

/** The original app's Go To Page options kept: a page number, or a juz plus the page inside that juz. */
@Composable
fun GoToPageForm(onOpen: (Int) -> Unit, modifier: Modifier = Modifier) {
    val index = LocalContainer.current.quranIndex
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var pageText by rememberSaveable { mutableStateOf("") }
    var juzText by rememberSaveable { mutableStateOf("") }
    var juzPageText by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }

    fun resolve(): Int? {
        if (mode == 0) return pageText.toIntOrNull()?.takeIf { PageCoordinates.isValid(it) }
        val juz = juzText.toIntOrNull() ?: return null
        val inJuz = juzPageText.toIntOrNull() ?: return null
        val start = index.juzByNumber(juz)?.startPage ?: return null
        if (inJuz !in 1..index.juzPageCount(juz)) return null
        return start + inJuz - 1
    }

    fun submit() {
        val page = resolve()
        if (page == null) showError = true else onOpen(page)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0; showError = false },
                label = { Text(stringResource(R.string.goto_mode_page)) }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1; showError = false },
                label = { Text(stringResource(R.string.goto_mode_juz)) }
            )
        }
        if (mode == 0) {
            NumberField(
                value = pageText,
                onValueChange = { pageText = it.filter(Char::isDigit).take(3); showError = false },
                label = stringResource(R.string.goto_page_hint),
                isError = showError,
                supporting = stringResource(if (showError) R.string.goto_error_page else R.string.goto_page_helper),
                modifier = Modifier.fillMaxWidth(),
                onGo = ::submit
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    value = juzText,
                    onValueChange = { juzText = it.filter(Char::isDigit).take(2); showError = false },
                    label = stringResource(R.string.goto_juz_hint),
                    isError = showError,
                    supporting = null,
                    modifier = Modifier.weight(1f),
                    onGo = ::submit
                )
                NumberField(
                    value = juzPageText,
                    onValueChange = { juzPageText = it.filter(Char::isDigit).take(2); showError = false },
                    label = stringResource(R.string.goto_juz_page_hint),
                    isError = showError,
                    supporting = null,
                    modifier = Modifier.weight(1f),
                    onGo = ::submit
                )
            }
            Text(
                text = stringResource(if (showError) R.string.goto_error_juz else R.string.goto_juz_helper),
                style = MaterialTheme.typography.bodySmall,
                color = if (showError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Button(onClick = ::submit) { Text(stringResource(R.string.goto_open)) }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    supporting: String?,
    modifier: Modifier,
    onGo: () -> Unit
) {
    val supportingContent: (@Composable () -> Unit)? = supporting?.let { text -> { Text(text) } }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingContent,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() })
    )
}
