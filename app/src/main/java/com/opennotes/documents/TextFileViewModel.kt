package com.opennotes.documents

import android.app.Application
import android.net.Uri
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.opennotes.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class TextFileViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val store = TextDocumentStore(application)
    private val draftId = savedState.get<String>("draftId") ?: UUID.randomUUID().toString().also { savedState["draftId"] = it }
    private val originalFile = File(application.filesDir, "external-drafts/$draftId.original")
    private val draftFile = File(application.filesDir, "external-drafts/$draftId.draft")
    private val draftMutex = Mutex()
    private var keepDraft = true
    val restoredUri get() = savedState.get<String>("uri")?.let(Uri::parse)
    private var original = byteArrayOf()
    private var format: TextFileFormat? = null
    var uri: Uri? = null
        private set
    var name by mutableStateOf("")
        private set
    var text by mutableStateOf("")
        private set
    var baseline by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var writable by mutableStateOf(false)
        private set
    var message by mutableStateOf<Int?>(null)
    val dirty get() = loaded && text != baseline

    fun open(target: Uri) {
        if (uri != null) return
        uri = target
        savedState["uri"] = target.toString()
        busy = true
        viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val filename = store.name(target)
                    require(TextFileCodec.supports(filename, getApplication<Application>().contentResolver.getType(target)))
                    val bytes = if (originalFile.exists() && draftFile.exists()) originalFile.readBytes() else store.read(target)
                    val decoded = TextFileCodec.decode(bytes)
                    originalFile.parentFile!!.mkdirs()
                    originalFile.writeBytes(bytes)
                    Triple(filename, bytes, decoded)
                }
                name = data.first
                original = data.second
                baseline = data.third.first
                format = data.third.second
                text = withContext(Dispatchers.IO) { if (draftFile.exists()) draftFile.readText() else baseline }
                writable = withContext(Dispatchers.IO) { store.canWrite(target) }
                loaded = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                message = if (e is TextFileTooLargeException) R.string.file_too_large else R.string.file_open_error
            } finally {
                busy = false
            }
        }
    }

    fun edit(value: String) {
        if (busy) return
        text = value
        // Keep drafts off the saved-instance-state Binder transaction, including large documents.
        viewModelScope.launch(Dispatchers.IO) {
            draftMutex.withLock {
                if (!keepDraft || text != value) return@withLock
                draftFile.parentFile!!.mkdirs()
                val file = AtomicFile(draftFile)
                val output = file.startWrite()
                try {
                    output.write(value.toByteArray(Charsets.UTF_8))
                    file.finishWrite(output)
                } catch (e: Exception) {
                    file.failWrite(output)
                }
            }
        }
    }

    fun save(copyTo: Uri? = null, onSuccess: () -> Unit = {}) {
        val target = copyTo ?: uri ?: return
        val encoding = format ?: return
        val savingText = text
        busy = true
        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    val encoded = encoding.encode(savingText)
                    store.write(target, encoded, if (copyTo == null) original else null)
                    encoded
                }
                if (copyTo == null) {
                    original = bytes
                    baseline = savingText
                    withContext(Dispatchers.IO) {
                        originalFile.writeBytes(bytes)
                        draftFile.writeText(text)
                    }
                }
                message = R.string.file_saved
                onSuccess()
            } catch (e: DocumentChangedException) {
                message = R.string.file_changed_error
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                message = if (e is TextFileTooLargeException) R.string.file_too_large else R.string.file_save_error
            } finally {
                busy = false
            }
        }
    }

    fun discardDraft() {
        keepDraft = false
        originalFile.delete()
        draftFile.delete()
    }
}
