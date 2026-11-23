package app.androfile.manager.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class DocumentStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("androfile.storage", Context.MODE_PRIVATE)

    fun savedRoot(): DocumentFile? {
        val raw = preferences.getString("tree_uri", null) ?: return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        return DocumentFile.fromTreeUri(context, uri)
    }

    fun saveRoot(uri: Uri, flags: Int) {
        val takeFlags = flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { context.contentResolver.takePersistableUriPermission(uri, takeFlags) }
        preferences.edit().putString("tree_uri", uri.toString()).apply()
    }

    fun list(parent: DocumentFile): List<DocumentFile> =
        parent.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name?.lowercase().orEmpty() })

    fun createFolder(parent: DocumentFile, name: String): DocumentFile {
        val safeName = name.trim()
        require(safeName.isNotEmpty()) { "Folder name cannot be empty." }
        require('/' !in safeName && '\\' !in safeName) { "Folder name contains an invalid character." }
        return parent.createDirectory(uniqueName(parent, safeName))
            ?: error("The selected storage provider could not create this folder.")
    }

    fun rename(document: DocumentFile, newName: String) {
        val safeName = newName.trim()
        require(safeName.isNotEmpty()) { "Name cannot be empty." }
        require('/' !in safeName && '\\' !in safeName) { "Name contains an invalid character." }
        check(document.renameTo(safeName)) { "The storage provider could not rename this item." }
    }

    fun delete(document: DocumentFile) {
        check(document.delete()) { "The storage provider could not delete this item." }
    }

    fun readText(document: DocumentFile, maxBytes: Int = 2 * 1024 * 1024): String {
        val input = context.contentResolver.openInputStream(document.uri) ?: error("Unable to read this file.")
        val output = ByteArrayOutputStream()
        input.use { source ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = source.read(buffer)
                if (count < 0) break
                total += count
                require(total <= maxBytes) { "This file is larger than the editor's 2 MB safety limit." }
                output.write(buffer, 0, count)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }

    fun writeText(document: DocumentFile, content: String) {
        val output = context.contentResolver.openOutputStream(document.uri, "wt")
            ?: error("Unable to open this file for writing.")
        output.buffered().use { it.write(content.toByteArray(Charsets.UTF_8)) }
    }

    fun copy(source: DocumentFile, destination: DocumentFile): DocumentFile = copyNode(source, destination)

    fun move(source: DocumentFile, destination: DocumentFile) {
        copyNode(source, destination)
        check(source.delete()) {
            "The copy completed, but the original could not be removed. Both copies were kept."
        }
    }

    private fun copyNode(source: DocumentFile, destination: DocumentFile): DocumentFile {
        val name = uniqueName(destination, source.name ?: "Untitled")
        if (source.isDirectory) {
            val target = destination.createDirectory(name) ?: error("Unable to create destination folder.")
            source.listFiles().forEach { child -> copyNode(child, target) }
            return target
        }
        val target = destination.createFile(mimeForName(name), name) ?: error("Unable to create destination file.")
        val input = context.contentResolver.openInputStream(source.uri)
            ?: error("Unable to read ${source.name ?: "source file"}.")
        val output = context.contentResolver.openOutputStream(target.uri, "w")
            ?: error("Unable to write ${target.name ?: "destination file"}.")
        input.use { sourceStream -> output.buffered().use { targetStream -> sourceStream.copyTo(targetStream) } }
        return target
    }

    fun createZip(source: DocumentFile, destination: DocumentFile): DocumentFile {
        val sourceName = source.name?.takeIf { it.isNotBlank() } ?: "files"
        val archiveName = uniqueName(destination, "$sourceName.zip")
        val archive = destination.createFile("application/zip", archiveName) ?: error("Unable to create ZIP archive.")
        val stream = context.contentResolver.openOutputStream(archive.uri, "w")
            ?: error("Unable to open the ZIP output stream.")
        try {
            ZipOutputStream(BufferedOutputStream(stream)).use { zip -> addToZip(source, sourceName, zip) }
        } catch (error: Exception) {
            archive.delete()
            throw error
        }
        return archive
    }

    private fun addToZip(node: DocumentFile, entryName: String, zip: ZipOutputStream) {
        if (node.isDirectory) {
            val children = node.listFiles()
            if (children.isEmpty()) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
            } else {
                children.forEach { child -> addToZip(child, "$entryName/${child.name ?: "item"}", zip) }
            }
        } else {
            zip.putNextEntry(ZipEntry(entryName))
            val input = context.contentResolver.openInputStream(node.uri)
                ?: error("Unable to read ${node.name ?: "archive source"}.")
            input.use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    fun extractZip(archive: DocumentFile, destination: DocumentFile): DocumentFile {
        require(archive.name?.endsWith(".zip", ignoreCase = true) == true) {
            "Only ZIP archives are supported in this Alpha."
        }
        val baseName = archive.name!!.removeSuffixIgnoreCase(".zip").ifBlank { "Extracted" }
        val folder = destination.createDirectory(uniqueName(destination, baseName))
            ?: error("Unable to create extraction folder.")
        var totalBytes = 0L
        var entries = 0
        try {
            val input = context.contentResolver.openInputStream(archive.uri) ?: error("Unable to read this ZIP archive.")
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries += 1
                    require(entries <= 10_000) { "Archive contains too many entries." }
                    val parts = safeZipPath(entry.name)
                    if (parts == null) {
                        zip.closeEntry()
                        continue
                    }
                    var parent = folder
                    val directoryParts = if (entry.isDirectory) parts else parts.dropLast(1)
                    directoryParts.forEach { segment ->
                        val existing = parent.findFile(segment)
                        parent = if (existing?.isDirectory == true) existing
                        else parent.createDirectory(uniqueName(parent, segment))
                            ?: error("Unable to create an extraction folder.")
                    }
                    if (!entry.isDirectory && parts.isNotEmpty()) {
                        val fileName = uniqueName(parent, parts.last())
                        val outputDocument = parent.createFile(mimeForName(fileName), fileName)
                            ?: error("Unable to create extracted file: $fileName")
                        val output = context.contentResolver.openOutputStream(outputDocument.uri, "w")
                            ?: error("Unable to write extracted file: $fileName")
                        output.buffered().use { target ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                require(totalBytes <= MAX_EXTRACT_BYTES) {
                                    "Archive expands beyond the 512 MB Alpha safety limit."
                                }
                                target.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: Exception) {
            folder.delete()
            throw error
        }
        return folder
    }

    private fun safeZipPath(raw: String): List<String>? {
        val normalized = raw.replace('\\', '/')
        if (normalized.startsWith('/') || Regex("^[A-Za-z]:").containsMatchIn(normalized)) return null
        val parts = normalized.split('/').filter { it.isNotBlank() }
        if (parts.isEmpty() || parts.any { it == "." || it == ".." || '\\u0000' in it }) return null
        return parts
    }

    private fun uniqueName(parent: DocumentFile, desired: String): String {
        val existing = parent.listFiles().mapNotNull { it.name }.toHashSet()
        if (desired !in existing) return desired
        val dot = desired.lastIndexOf('.')
        val base = if (dot > 0) desired.substring(0, dot) else desired
        val extension = if (dot > 0) desired.substring(dot) else ""
        var index = 2
        while ("$base ($index)$extension" in existing) index++
        return "$base ($index)$extension"
    }

    private fun mimeForName(name: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun String.removeSuffixIgnoreCase(suffix: String): String =
        if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this

    companion object {
        private const val MAX_EXTRACT_BYTES = 512L * 1024L * 1024L
    }
}
