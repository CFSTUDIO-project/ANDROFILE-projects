package app.androfile.manager.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CodeEditorScreen(
    fileName: String,
    initialText: String,
    accent: Color,
    onBack: () -> Unit,
    onSave: (String) -> Unit,
    onCopyAll: (String) -> Unit,
    onPreview: ((String) -> Unit)?
) {
    var code by remember(fileName, initialText) { mutableStateOf(initialText) }
    var showDiscard by remember { mutableStateOf(false) }
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val changed = code != initialText

    fun leave() {
        if (changed) showDiscard = true else onBack()
    }
    BackHandler(enabled = true) { leave() }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF10151E)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { leave() }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(fileName, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                Text("${extension.uppercase().ifBlank { "TEXT" }}  •  ${code.lines().size} lines",
                    style = MaterialTheme.typography.labelSmall, color = Color(0xFF91A0B3))
            }
            IconButton(onClick = { onCopyAll(code) }) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy all", tint = Color(0xFFBBC8D8))
            }
        }
        Surface(color = Color(0xFF18212D), shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                textStyle = TextStyle(
                    color = Color(0xFFE6EDF5),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                ),
                visualTransformation = SyntaxHighlighter(extension),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                minLines = 20,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = accent,
                    focusedTextColor = Color(0xFFE6EDF5),
                    unfocusedTextColor = Color(0xFFE6EDF5)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(code) },
                enabled = changed,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF092219)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Rounded.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
            if (extension == "html" || extension == "htm") {
                Button(
                    onClick = { onPreview?.invoke(code) },
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27394B), contentColor = Color.White),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Preview")
                }
            }
        }
        if (changed) Text("Unsaved changes", color = Color(0xFFE7B36A),
            style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = Color(0xFFE7B36A)) },
            title = { Text("Discard changes?") },
            text = { Text("Your edits have not been saved. Leave the editor and discard them?") },
            confirmButton = { TextButton(onClick = { showDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { showDiscard = false }) { Text("Keep editing") } },
            containerColor = Color(0xFF202A38),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB9C4D2)
        )
    }
}
