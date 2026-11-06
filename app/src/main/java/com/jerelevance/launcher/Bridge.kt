package com.jerelevance.launcher

import android.app.SearchManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * window.JRLAndroid in the page. Every method is called on a WebView binder thread,
 * so anything that touches views or starts activities hops to the UI thread.
 */
class Bridge(private val act: MainActivity) {

    private val pm get() = act.packageManager
    private fun ui(block: () -> Unit) = act.runOnUiThread(block)

    /** Launchable activities: [{key:"pkg/class", pkg, label, sys}] */
    @JavascriptInterface
    fun apps(): String {
        val out = JSONArray()
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        for (ri in pm.queryIntentActivities(q, 0)) {
            val ai = ri.activityInfo ?: continue
            if (ai.packageName == act.packageName) continue
            out.put(
                JSONObject()
                    .put("key", ai.packageName + "/" + ai.name)
                    .put("pkg", ai.packageName)
                    .put("label", ri.loadLabel(pm).toString())
                    .put("sys", (ai.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
            )
        }
        return out.toString()
    }

    /** Rect is in physical pixels relative to the WebView; zero size means no reveal animation. */
    @JavascriptInterface
    fun launch(key: String, x: Int, y: Int, w: Int, h: Int) {
        val r = if (w > 0 && h > 0) Rect(x, y, x + w, y + h) else null
        ui { act.launch(key, r) }
    }

    @JavascriptInterface
    fun info(key: String) = ui {
        act.start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", key.substringBefore('/'), null)))
    }

    @JavascriptInterface
    fun uninstall(key: String) = ui {
        act.start(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", key.substringBefore('/'), null)))
    }

    @JavascriptInterface
    fun battery(): String {
        val bm = act.getSystemService(BatteryManager::class.java)
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return JSONObject().put("level", if (level in 0..100) level else -1).put("charging", bm?.isCharging ?: false).toString()
    }

    /** {on, count, pkgs:{pkg:n}}; on=false until the user grants notification access. */
    @JavascriptInterface
    fun notifs(): String {
        val on = NotificationManagerCompat.getEnabledListenerPackages(act).contains(act.packageName)
        val o = JSONObject().put("on", on)
        if (!on) return o.put("count", -1).put("pkgs", JSONObject()).toString()
        val counts = NotifListener.counts
        val pk = JSONObject()
        counts.forEach { (k, v) -> pk.put(k, v) }
        return o.put("count", counts.values.sum()).put("pkgs", pk).toString()
    }

    @JavascriptInterface
    fun device(): String {
        val version = try { pm.getPackageInfo(act.packageName, 0).versionName ?: "?" } catch (_: Exception) { "?" }
        return JSONObject()
            .put("maker", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("release", Build.VERSION.RELEASE)
            .put("sdk", Build.VERSION.SDK_INT)
            .put("uptime", SystemClock.elapsedRealtime() / 1000)
            .put("version", version)
            .toString()
    }

    @JavascriptInterface
    fun haptic(kind: String) = ui {
        act.web.performHapticFeedback(
            when (kind) {
                "long" -> HapticFeedbackConstants.LONG_PRESS
                "tick" -> HapticFeedbackConstants.CLOCK_TICK
                else -> HapticFeedbackConstants.VIRTUAL_KEY
            }
        )
    }

    @JavascriptInterface
    fun settings(which: String): Boolean {
        val action = when (which.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bt", "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Intent.ACTION_POWER_USAGE_SUMMARY
            "launcher", "home" -> Settings.ACTION_HOME_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "notif" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "wallpaper" -> Intent.ACTION_SET_WALLPAPER
            "date" -> Settings.ACTION_DATE_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "airplane" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "dev" -> Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            "data" -> Settings.ACTION_DATA_ROAMING_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        ui { act.start(Intent(action)) }
        return true
    }

    /** Pulls down the notification shade. Uses a hidden API; does nothing if the phone refuses. */
    @JavascriptInterface
    fun expand() = ui {
        try {
            @Suppress("WrongConstant")
            val sb = act.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (_: Throwable) {
        }
    }

    @JavascriptInterface
    fun webSearch(q: String) = ui {
        val ok = act.start(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q))
        if (!ok) act.openExternal(Uri.parse("https://www.google.com/search?q=" + Uri.encode(q)))
    }

    @JavascriptInterface
    fun view(url: String) = ui { act.openExternal(Uri.parse(url)) }

    /** dark = dark status/nav bar icons (for light fields). */
    @JavascriptInterface
    fun bars(dark: Boolean) = ui { act.setDarkBarIcons(dark) }

    @JavascriptInterface
    fun keyboard() = ui { act.showKeyboard() }
}
