package app.androfile.manager.ui
import android.content.Context
data class AppAppearance(val background: String = "midnight", val accent: String = "mint")
object AppPreferences {
 fun load(context: Context): AppAppearance {
  val prefs = context.getSharedPreferences("androfile.appearance", Context.MODE_PRIVATE)
  return AppAppearance(
   background = prefs.getString("background", "midnight") ?: "midnight",
   accent = prefs.getString("accent", "mint") ?: "mint"
  )
 }
 fun save(context: Context, appearance: AppAppearance) {
  context.getSharedPreferences("androfile.appearance", Context.MODE_PRIVATE).edit()
   .putString("background", appearance.background)
   .putString("accent", appearance.accent)
   .apply()
 }
}
