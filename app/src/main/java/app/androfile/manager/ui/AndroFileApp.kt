package app.androfile.manager.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import app.androfile.manager.data.DocumentStore
import app.androfile.manager.data.ShizukuAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val editableExtensions = setOf(
    "txt", "md", "html", "htm", "css", "js", "mjs", "ts", "json", "xml",
    "kt", "kts", "java", "py", "gradle", "properties", "yml", "yaml",
    "sh", "log", "csv", "toml", "ini", "conf", "sql", "c", "h", "cpp", "rs"
)

@Composable
fun AndroFileApp() {
    val context = LocalContext.current
    val store = remember { DocumentStore(context) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var appearance by remember { mutableStateOf(AppPreferences.load(context)) }
    val palette = remember(appearance) { paletteFor(appearance) }
    var root by remember { mutableStateOf(store.savedRoot()) }
    val directoryStack = remember { androidx.compose.runtime.mutableStateListOf<DocumentFile>() }
    val currentDirectory = directoryStack.lastOrNull() ?: root
    var fileItems by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var refreshCounter by remember { mutableIntStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    var editorDocument by remember { mutableStateOf<DocumentFile?>(null) }
    var editorInitialText by remember { mutableStateOf("") }
    var editorMode by remember { mutableStateOf("main") }
    var previewHtml by remember { mutableStateOf("") }

    var renameTarget by remember { mutableStateOf<DocumentFile?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<DocumentFile?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var pendingDocument by remember { mutableStateOf<DocumentFile?>(null) }
    var pendingMove by remember { mutableStateOf(false) }
    var shizukuStatus by remember { mutableStateOf(ShizukuAccess.readStatus()) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            store.saveRoot(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            root = DocumentFile.fromTreeUri(context, uri)
            directoryStack.clear()
            searchText = ""
            refreshCounter++
        }
    }

    LaunchedEffect(currentDirectory?.uri?.toString(), refreshCounter) {
        val directory = currentDirectory
        fileItems = if (directory == null) emptyList() else {
            runCatching { withContext(Dispatchers.IO) { store.list(directory) } }
                .getOrElse {
                    snackbar.showSnackbar(it.message ?: "Unable to read this folder.")
                    emptyList()
                }
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun performOperation(
        successMessage: String,
        action: () -> Unit,
        onSuccess: () -> Unit = {}
    ) {
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { action() } }
            if (result.isSuccess) {
                refreshCounter++
                onSuccess()
            }
            snackbar.showSnackbar(result.fold(
                onSuccess = { successMessage },
                onFailure = { it.message ?: "The operation could not be completed." }
            ))
        }
    }

    fun openEditor(document: DocumentFile) {
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { store.readText(document) } }
            result.onSuccess {
                editorDocument = document
                editorInitialText = it
                editorMode = "editor"
            }.onFailure {
                snackbar.showSnackbar(it.message ?: "Unable to open this file in the editor.")
            }
        }
    }

    fun openDocument(document: DocumentFile) {
        if (document.isDirectory) {
            directoryStack.add(document)
            searchText = ""
            return
        }
        val extension = document.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (extension in editableExtensions) {
            openEditor(document)
            return
        }
        val mime = context.contentResolver.getType(document.uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(document.uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No installed app can open this file.", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open this file.", Toast.LENGTH_SHORT).show()
        }
    }

    fun handleFileAction(action: String, document: DocumentFile) {
        when (action) {
            "edit" -> openEditor(document)
            "rename" -> {
                renameTarget = document
                renameValue = document.name.orEmpty()
            }
            "delete" -> deleteTarget = document
            "copy" -> {
                pendingDocument = document
                pendingMove = false
                showMessage("Copy ready. Open the destination folder and tap Paste.")
            }
            "move" -> {
                pendingDocument = document
                pendingMove = true
                showMessage("Move ready. Open the destination folder and tap Paste.")
            }
            "compress" -> {
                val destination = currentDirectory ?: return
                performOperation("ZIP archive created.") { store.createZip(document, destination) }
            }
            "extract" -> {
                val destination = currentDirectory ?: return
                performOperation("ZIP archive extracted.") { store.extractZip(document, destination) }
            }
        }
    }

    if (editorMode == "editor" && editorDocument != null) {
        CodeEditorScreen(
            fileName = editorDocument?.name ?: "Untitled",
            initialText = editorInitialText,
            accent = palette.accent,
            onBack = { editorMode = "main" },
            onSave = { editedText ->
                val document = editorDocument
                if (document != null) {
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) { store.writeText(document, editedText) }
                        }
                        result.onSuccess {
                            editorInitialText = editedText
                            editorMode = "main"
                            refreshCounter++
                            snackbar.showSnackbar("File saved to storage.")
                        }.onFailure {
                            snackbar.showSnackbar(it.message ?: "Could not save this file.")
                        }
                    }
                }
            },
            onCopyAll = { text ->
                clipboard.setText(AnnotatedString(text))
                showMessage("All code copied.")
            },
            onPreview = { html ->
                previewHtml = html
                editorMode = "preview"
            }
        )
        return
    }

    if (editorMode == "preview" && editorDocument != null) {
        HtmlPreviewScreen(
            fileName = editorDocument?.name ?: "Preview",
            html = previewHtml,
            onBack = { editorMode = "editor" },
            onEdit = {
                editorInitialText = previewHtml
                editorMode = "editor"
            }
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = palette.surface, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Files") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; shizukuStatus = ShizukuAccess.readStatus() },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { androidx.compose.material3.Text("Settings") }
                )
            }
        }
    ) { insets ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(palette.background, palette.backgroundShade, Color(0xFF0B1017))))
                .padding(insets)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTopBar(palette)
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        (fadeIn() + slideInVertically { it / 28 }) togetherWith
                            (fadeOut() + slideOutVertically { -it / 28 })
                    },
                    label = "androfile-tab"
                ) { tab ->
                    if (tab == 1) {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsPanel(
                                appearance = appearance,
                                palette = palette,
                                shizuku = shizukuStatus,
                                onAppearanceChange = {
                                    appearance = it
                                    AppPreferences.save(context, it)
                                },
                                onRefreshShizuku = { shizukuStatus = ShizukuAccess.readStatus() },
                                onRequestShizuku = {
                                    if (!ShizukuAccess.requestPermission()) {
                                        showMessage("Shizuku Binder is not available. Start Shizuku, then refresh.")
                                    } else {
                                        showMessage("Permission request sent to Shizuku.")
                                    }
                                    shizukuStatus = ShizukuAccess.readStatus()
                                }
                            )
                        }
                    } else {
                        val visibleFiles = fileItems.filter {
                            it.name?.contains(searchText, ignoreCase = true) == true
                        }
                        FileBrowserPanel(
                            palette = palette,
                            directory = currentDirectory,
                            canGoUp = directoryStack.isNotEmpty(),
                            fileItems = visibleFiles,
                            searchText = searchText,
                            hasPending = pendingDocument != null,
                            pendingMove = pendingMove,
                            onSearch = { searchText = it },
                            onUp = {
                                if (directoryStack.isNotEmpty()) directoryStack.removeAt(directoryStack.lastIndex)
                            },
                            onChooseFolder = { folderPicker.launch(null) },
                            onCreateFolder = { newFolderName = ""; showNewFolderDialog = true },
                            onOpen = { openDocument(it) },
                            onAction = { action, document -> handleFileAction(action, document) },
                            onPaste = {
                                val source = pendingDocument
                                val destination = currentDirectory
                                if (source != null && destination != null) {
                                    val moving = pendingMove
                                    performOperation(
                                        if (moving) "File moved." else "File copied.",
                                        action = {
                                            if (moving) store.move(source, destination) else store.copy(source, destination)
                                        },
                                        onSuccess = { pendingDocument = null }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (renameTarget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { androidx.compose.material3.Text("Rename item", color = palette.text) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("New name") },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = Color(0xFF475668),
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val target = renameTarget
                    if (target != null) performOperation("Item renamed.") { store.rename(target, renameValue) }
                    renameTarget = null
                }) { androidx.compose.material3.Text("Save", color = palette.accent) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renameTarget = null }) { androidx.compose.material3.Text("Cancel") } },
            containerColor = palette.surface
        )
    }

    if (showNewFolderDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { androidx.compose.material3.Text("Create folder", color = palette.text) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { androidx.compose.material3.Text("Folder name") },
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = Color(0xFF475668),
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val destination = currentDirectory
                    if (destination != null) performOperation("Folder created.") {
                        store.createFolder(destination, newFolderName)
                    }
                    showNewFolderDialog = false
                }) { androidx.compose.material3.Text("Create", color = palette.accent) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showNewFolderDialog = false }) { androidx.compose.material3.Text("Cancel") } },
            containerColor = palette.surface
        )
    }

    if (deleteTarget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { androidx.compose.material3.Text("Delete this item?", color = palette.text) },
            text = {
                androidx.compose.material3.Text(
                    "This removes the real item from the selected storage folder. This action cannot be undone.",
                    color = palette.muted
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val target = deleteTarget
                    if (target != null) performOperation("Item deleted from storage.") { store.delete(target) }
                    deleteTarget = null
                }) { androidx.compose.material3.Text("Delete", color = Color(0xFFE28A91)) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { deleteTarget = null }) { androidx.compose.material3.Text("Cancel") } },
            containerColor = palette.surface
        )
    }
}
