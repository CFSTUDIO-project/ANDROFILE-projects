package app.androfile.manager.ui

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import java.io.ByteArrayInputStream

@Composable
fun HtmlPreviewScreen(fileName: String, html: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val pageKey = remember(html) { html.hashCode().toString() }
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF10151E)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("HTML Preview", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(fileName, color = Color(0xFF91A0B3), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Code, contentDescription = "Edit code", tint = Color(0xFF80DDBB))
            }
        }
        Surface(color = Color(0xFF18212D), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
            Text("LOCAL PREVIEW  •  Filesystem access blocked",
                color = Color(0xFF9DB1C6), style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.allowFileAccessFromFileURLs = false
                    settings.allowUniversalAccessFromFileURLs = false
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val host = request.url.host
                            return host != "androfile-preview.local" && request.url.scheme != "data" &&
                                request.url.scheme != "about"
                        }
                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val host = request.url.host
                            if ((request.url.scheme == "http" || request.url.scheme == "https") && host != "androfile-preview.local") {
                                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                            }
                            return super.shouldInterceptRequest(view, request)
                        }
                    }
                    setBackgroundColor(android.graphics.Color.WHITE)
                }
            },
            update = { webView ->
                if (webView.tag != pageKey) {
                    webView.tag = pageKey
                    webView.loadDataWithBaseURL("https://androfile-preview.local/", html, "text/html", "UTF-8", null)
                }
            }
        )
    }
}
