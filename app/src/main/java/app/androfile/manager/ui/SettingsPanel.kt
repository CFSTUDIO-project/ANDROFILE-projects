package app.androfile.manager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.androfile.manager.data.ShizukuStatus

@Composable
fun SettingsPanel(
    appearance: AppAppearance,
    palette: AppPalette,
    shizuku: ShizukuStatus,
    onAppearanceChange: (AppAppearance) -> Unit,
    onRefreshShizuku: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Personalise your workspace", color = palette.text, style = MaterialTheme.typography.headlineSmall)
            Text("A little colour, without losing the focus.", color = palette.muted, style = MaterialTheme.typography.bodyMedium)
        }

        Surface(color = palette.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(10.dp))
                    Text("Background", color = palette.text, style = MaterialTheme.typography.titleMedium)
                }
                Text("Choose the atmosphere for your file workspace.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple("midnight", "Midnight", Color(0xFF10151E)),
                        Triple("graphite", "Graphite", Color(0xFF17191F)),
                        Triple("ocean", "Ocean", Color(0xFF0C1A29)),
                        Triple("plum", "Plum", Color(0xFF1B1526))
                    ).forEach { (id, title, colour) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f).clickable {
                                onAppearanceChange(appearance.copy(background = id))
                            }
                        ) {
                            Surface(
                                color = colour,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().size(48.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (appearance.background == id) 2.dp else 1.dp,
                                    if (appearance.background == id) palette.accent else Color(0xFF3B4858)
                                )
                            ) {
                                if (appearance.background == id) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected",
                                        tint = palette.accent, modifier = Modifier.padding(13.dp))
                                }
                            }
                            Text(title, color = palette.muted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("Accent colour", color = palette.text, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        "mint" to Color(0xFF70DDB5),
                        "cyan" to Color(0xFF69C9F0),
                        "violet" to Color(0xFFB18BF3),
                        "amber" to Color(0xFFE8B76E)
                    ).forEach { (id, colour) ->
                        Surface(
                            color = colour,
                            shape = CircleShape,
                            modifier = Modifier.size(34.dp).clickable {
                                onAppearanceChange(appearance.copy(accent = id))
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                if (appearance.accent == id) 3.dp else 0.dp,
                                Color.White
                            )
                        ) {}
                    }
                }
                Text("File icons use distinct colours by file type.", color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        Surface(color = palette.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(10.dp))
                    Text("Shizuku verification", color = palette.text, style = MaterialTheme.typography.titleMedium)
                }
                val statusText = when {
                    shizuku.permissionGranted -> "Connected · Permission granted"
                    shizuku.connected -> "Service connected · Permission needed"
                    else -> "Shizuku service not detected"
                }
                val statusColour = when {
                    shizuku.permissionGranted -> Color(0xFF69D7A6)
                    shizuku.connected -> Color(0xFFE8B76E)
                    else -> Color(0xFFDB7C86)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Surface(color = statusColour, shape = CircleShape, modifier = Modifier.size(9.dp)) {}
                    Text(statusText, color = statusColour, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    "AndroFile checks the live Shizuku Binder and this app's permission grant. File operations in Alpha still use the folder access you approve in Android.",
                    color = palette.muted, style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRequestShizuku,
                        enabled = shizuku.connected && !shizuku.permissionGranted,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color(0xFF08261B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Security, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(if (shizuku.permissionGranted) "Granted" else "Request access")
                    }
                    OutlinedButton(onClick = onRefreshShizuku) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
                if (!shizuku.connected) {
                    Text("Start Shizuku first, then tap Refresh. On supported Android versions it can be started with Wireless debugging.",
                        color = palette.muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Surface(color = palette.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(10.dp))
                    Text("Storage access", color = palette.text, style = MaterialTheme.typography.titleMedium)
                }
                Text("This Alpha uses Android's system folder picker. Only the folder you select is available to AndroFile; protected Android and app-private areas remain restricted by the operating system.",
                    color = palette.muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
