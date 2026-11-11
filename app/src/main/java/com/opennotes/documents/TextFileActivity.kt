package com.opennotes.documents

import android.content.Intent
import android.content.Context
import android.webkit.MimeTypeMap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.opennotes.R
import com.opennotes.settings.presentation.SettingsViewModel
import com.opennotes.ui.theme.OpenNotesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TextFileActivity : ComponentActivity() {
    private val editor: TextFileViewModel by viewModels()
    private val settings: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Editor() }
    }

    private fun open(uri: Uri, flags: Int) {
        if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
            }
        }
        editor.open(uri)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Editor() {
        val currentSettings by settings.settings.collectAsState()
        val snackbars = remember { SnackbarHostState() }
        var confirmClose by remember { mutableStateOf(false) }
        var closeAfterCopy by remember { mutableStateOf(false) }
        val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (result.resultCode == RESULT_OK && uri != null) open(uri, result.data!!.flags) else finish()
        }
        val saveCopy = rememberLauncherForActivityResult(CreateTextDocument()) { uri ->
            if (uri != null) editor.save(uri) {
                if (closeAfterCopy) { editor.discardDraft(); finish() }
            }
            else closeAfterCopy = false
        }
        fun requestClose() {
            if (editor.busy) return
            if (editor.dirty) confirmClose = true else { editor.discardDraft(); finish() }
        }

        LaunchedEffect(Unit) {
            val uri = editor.restoredUri ?: intent.data
            if (uri != null) open(uri, intent.flags) else chooseFile.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                },
            )
        }
        val message = editor.message
        LaunchedEffect(message) {
            message?.let { snackbars.showSnackbar(getString(it)); editor.message = null }
        }
        BackHandler { requestClose() }
        OpenNotesTheme(currentSettings) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbars) },
                topBar = {
                    TopAppBar(
                        title = { Text(editor.name.ifEmpty { stringResource(R.string.open_text_file) }, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = { requestClose() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back_desc))
                            }
                        },
                        actions = {
                            IconButton(enabled = editor.loaded && !editor.busy, onClick = { saveCopy.launch(editor.name) }) {
                                Icon(Icons.Default.SaveAs, stringResource(R.string.save_file_copy))
                            }
                            IconButton(enabled = editor.dirty && editor.writable && !editor.busy, onClick = { editor.save { setResult(RESULT_OK, Intent().setData(editor.uri)) } }) {
                                Icon(Icons.Default.Save, stringResource(R.string.save_text_file))
                            }
                        },
                    )
                },
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(12.dp)) {
                    if (editor.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (editor.loaded) {
                        Text(
                            stringResource(if (!editor.writable) R.string.file_read_only else if (editor.dirty) R.string.file_unsaved else R.string.file_ready),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        OutlinedTextField(
                            value = editor.text,
                            onValueChange = editor::edit,
                            enabled = !editor.busy,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    } else if (!editor.busy) {
                        Text(stringResource(R.string.file_open_error))
                    }
                }
            }
            if (confirmClose) {
                AlertDialog(
                    onDismissRequest = { confirmClose = false },
                    title = { Text(stringResource(R.string.file_unsaved)) },
                    text = { Text(stringResource(R.string.file_unsaved_prompt)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmClose = false
                            if (editor.writable) editor.save { setResult(RESULT_OK, Intent().setData(editor.uri)); editor.discardDraft(); finish() }
                            else { closeAfterCopy = true; saveCopy.launch(editor.name) }
                        }) { Text(stringResource(if (editor.writable) R.string.save_text_file else R.string.save_file_copy)) }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = { confirmClose = false }) { Text(stringResource(R.string.cancel)) }
                            TextButton(onClick = { editor.discardDraft(); finish() }) { Text(stringResource(R.string.discard_file_changes)) }
                        }
                    },
                )
            }
        }
    }
}

private class CreateTextDocument : ActivityResultContracts.CreateDocument("text/plain") {
    override fun createIntent(context: Context, input: String): Intent {
        val extension = input.substringAfterLast('.', "").lowercase()
        val mime = when (extension) {
            "md", "markdown" -> "text/markdown"
            "yaml", "yml" -> "application/yaml"
            "toml" -> "application/toml"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "text/plain"
        }
        return super.createIntent(context, input).setType(mime)
    }
}
