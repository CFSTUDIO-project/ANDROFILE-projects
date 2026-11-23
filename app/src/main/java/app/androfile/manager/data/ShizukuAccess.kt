package app.androfile.manager.data

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

data class ShizukuStatus(val connected: Boolean, val permissionGranted: Boolean)

object ShizukuAccess {
    const val REQUEST_CODE = 4107

    fun readStatus(): ShizukuStatus = runCatching {
        val connected = Shizuku.pingBinder()
        val granted = connected && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        ShizukuStatus(connected, granted)
    }.getOrElse { ShizukuStatus(false, false) }

    fun requestPermission(): Boolean = runCatching {
        if (!Shizuku.pingBinder()) return false
        Shizuku.requestPermission(REQUEST_CODE)
        true
    }.getOrDefault(false)
}
