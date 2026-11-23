package app.androfile.manager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.androfile.manager.ui.AndroFileApp
import app.androfile.manager.ui.AndroFileTheme
class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  setContent { AndroFileTheme { AndroFileApp() } }
 }
}
