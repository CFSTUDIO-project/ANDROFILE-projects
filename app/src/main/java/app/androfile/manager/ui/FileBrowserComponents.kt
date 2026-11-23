package app.androfile.manager.ui

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import app.androfile.manager.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AppTopBar(palette: AppPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(color = palette.surfaceRaised, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(48.dp)) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.androfile_foreground),
                contentDescription = "AndroFile logo",
                modifier = Modifier.padding(3.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("ANDROFILE", color = palette.text, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontSize = 19.sp)
            Text("ANDROID FILE WORKSPACE", color = palette.muted, fontSize = 9.sp, letterSpacing = 1.35.sp)
        }
        Surface(color = palette.accentSoft, shape = RoundedCornerShape(20.dp)) {
            Text("1.0 ALPHA", color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
        }
    }
}

@Composable
fun FileBrowserPanel(
    palette: AppPalette,
    directory: DocumentFile?,
    canGoUp: Boolean,
    fileItems: List<DocumentFile>,
    searchText: String,
    hasPending: Boolean,
    pendingMove: Boolean,
    onSearch: (String) -> Unit,
    onUp: () -> Unit,
    onChooseFolder: () -> Unit,
    onCreateFolder: () -> Unit,
    onOpen: (DocumentFile) -> Unit,
    onAction: (String, DocumentFile) -> Unit,
    onPaste: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canGoUp) IconButton(onClick = onUp) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Parent folder", tint = palette.text)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(directory?.name ?: "My files", color = palette.text, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Text(if (canGoUp) "Inside selected folder" else "Your storage workspace",
                    color = palette.muted, style = MaterialTheme.typography.labelSmall)
            }
            if (hasPending) {
                Button(
                    onClick = onPaste,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF092219)),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(if (pendingMove) Icons.Rounded.ContentCut else Icons.Rounded.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Paste")
                }
            } else if (directory != null) {
                IconButton(onClick = onCreateFolder) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = "New folder", tint = palette.accent)
                }
            }
        }

        if (directory == null) {
            EmptyFolderState(palette, onChooseFolder)
            return
        }

        val stats = remember {
            runCatching {
                val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val total = stat.totalBytes
                val free = stat.availableBytes
                Triple(total, free, if (total > 0) (total - free).toFloat() / total.toFloat() else 0f)
            }.getOrElse { Triple(0L, 0L, 0f) }
        }
        StorageCard(palette, stats.first, stats.second, stats.third)

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            placeholder = { Text("Search in this folder", color = palette.muted) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.muted) },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearch("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = Color(0xFF344356),
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
                cursorColor = palette.accent
            ),
            shape = RoundedCornerShape(16.dp)
        )

        if (fileItems.isEmpty()) {
            EmptySearchState(palette, searchText.isNotBlank())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(fileItems, key = { it.uri.toString() }) { document ->
                    FileRow(
                        document = document,
                        palette = palette,
                        onOpen = { onOpen(document) },
                        onAction = { action -> onAction(action, document) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageCard(palette: AppPalette, totalBytes: Long, freeBytes: Long, usedRatio: Float) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.035f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = palette.accentSoft, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = palette.accent, modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device storage", color = palette.text, fontWeight = FontWeight.SemiBold)
                    Text(if (totalBytes > 0) "${formatBytes(freeBytes)} available of ${formatBytes(totalBytes)}" else "Storage details unavailable",
                        color = palette.muted, style = MaterialTheme.typography.labelSmall)
                }
                Text(if (totalBytes > 0) "${(usedRatio * 100).toInt()}%" else "—", color = palette.accent, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = { usedRatio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = palette.accent,
                trackColor = Color(0xFF344356)
            )
        }
    }
}

@Composable
private fun EmptyFolderState(palette: AppPalette, onChoose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(color = palette.accentSoft, shape = RoundedCornerShape(26.dp), modifier = Modifier.size(84.dp)) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = palette.accent, modifier = Modifier.padding(23.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("Your files, in one place.", color = palette.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose a folder to start browsing. AndroFile will request access through Android's own folder picker.",
            color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onChoose,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF092219)),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose a folder", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text("You can change the folder at any time from the system picker.",
            color = palette.muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EmptySearchState(palette: AppPalette, searching: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.FolderOff, contentDescription = null, tint = palette.muted, modifier = Modifier.size(38.dp))
        Text(if (searching) "No matching files" else "This folder is empty", color = palette.text, fontWeight = FontWeight.SemiBold)
        Text(if (searching) "Try another search term." else "Create a folder or add files from another app.",
            color = palette.muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FileRow(
    document: DocumentFile,
    palette: AppPalette,
    onOpen: () -> Unit,
    onAction: (String) -> Unit
) {
    var menuExpanded by remember(document.uri) { mutableStateOf(false) }
    val name = document.name ?: "Untitled"
    val extension = name.substringAfterLast('.', "").lowercase()
    val (icon, tint) = fileIcon(name, document.isDirectory)
    val subtitle = when {
        document.isDirectory -> "${document.listFiles().size} items"
        document.lastModified() > 0L -> "${formatBytes(document.length())} · ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(document.lastModified()))}"
        else -> formatBytes(document.length())
    }

    Surface(
        color = palette.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.025f)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = tint.copy(alpha = 0.14f), shape = RoundedCornerShape(13.dp), modifier = Modifier.size(44.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, color = palette.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, color = palette.muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "File actions", tint = palette.muted)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (!document.isDirectory && extension in setOf(
                            "txt", "md", "html", "htm", "css", "js", "ts", "json", "xml", "kt", "kts", "java", "py", "gradle", "properties", "yaml", "yml", "sh", "csv", "toml", "ini", "sql"
                        )
                    ) {
                        DropdownMenuItem(text = { Text("Edit file") }, leadingIcon = { Icon(Icons.Rounded.Code, null) },
                            onClick = { menuExpanded = false; onAction("edit") })
                    }
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menuExpanded = false; onAction("rename") })
                    DropdownMenuItem(text = { Text("Copy") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = { menuExpanded = false; onAction("copy") })
                    DropdownMenuItem(text = { Text("Move") }, leadingIcon = { Icon(Icons.Rounded.ContentCut, null) },
                        onClick = { menuExpanded = false; onAction("move") })
                    if (!document.isDirectory && extension == "zip") {
                        DropdownMenuItem(text = { Text("Extract ZIP here") }, leadingIcon = { Icon(Icons.Rounded.Unarchive, null) },
                            onClick = { menuExpanded = false; onAction("extract") })
                    } else {
                        DropdownMenuItem(text = { Text("Compress to ZIP") }, leadingIcon = { Icon(Icons.Rounded.Archive, null) },
                            onClick = { menuExpanded = false; onAction("compress") })
                    }
                    DropdownMenuItem(text = { Text("Delete", color = Color(0xFFE28A91)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Color(0xFFE28A91)) },
                        onClick = { menuExpanded = false; onAction("delete") })
                }
            }
        }
    }
}

private fun fileIcon(name: String, isDirectory: Boolean): Pair<ImageVector, Color> {
    if (isDirectory) return Icons.Rounded.Folder to Color(0xFF79B9F5)
    return when (name.substringAfterLast('.', "").lowercase()) {
        "zip", "7z", "rar", "tar", "gz" -> Icons.Rounded.Archive to Color(0xFFB99AF5)
        "html", "htm", "js", "ts", "css", "kt", "kts", "java", "py", "json", "xml", "gradle" ->
            Icons.Rounded.Code to Color(0xFF72D4C3)
        "png", "jpg", "jpeg", "webp", "gif", "svg" -> Icons.Rounded.Image to Color(0xFFE59AC2)
        "mp3", "wav", "ogg", "flac", "m4a" -> Icons.Rounded.Audiotrack to Color(0xFF7ED6A5)
        "mp4", "mkv", "webm", "mov" -> Icons.Rounded.Movie to Color(0xFFF0B36C)
        "pdf" -> Icons.Rounded.PictureAsPdf to Color(0xFFE58B91)
        "apk" -> Icons.Rounded.Android to Color(0xFF8DD68D)
        "csv", "xls", "xlsx" -> Icons.Rounded.TableChart to Color(0xFF7EC8A8)
        else -> Icons.Rounded.Description to Color(0xFFAAB7C7)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do {
        value /= 1024.0
        index++
    } while (value >= 1024.0 && index < units.lastIndex)
    return String.format(Locale.getDefault(), "%.1f %s", value, units[index])
}
