package com.jerelevance.launcher

import android.Manifest
import android.app.SearchManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.CalendarContract
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
import androidx.core.content.ContextCompat
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

    // ---- calendar: upcoming events from every visible calendar on the phone (read-only) ----

    @JavascriptInterface
    fun events(days: Int): String {
        val out = JSONObject()
        if (ContextCompat.checkSelfPermission(act, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            return out.put("perm", false).put("ev", JSONArray()).toString()
        val arr = JSONArray()
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now - 12 * 3600_000L)
            ContentUris.appendId(it, now + days.coerceIn(1, 31) * 86400_000L)
        }.build()
        val proj = arrayOf(
            CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY
        )
        try {
            act.contentResolver.query(uri, proj, CalendarContract.Instances.VISIBLE + "=1", null, CalendarContract.Instances.BEGIN + " ASC")?.use { c ->
                while (c.moveToNext() && arr.length() < 40) {
                    val end = c.getLong(3)
                    if (end <= now) continue
                    arr.put(JSONObject().put("id", c.getLong(0).toString()).put("title", c.getString(1) ?: "")
                        .put("begin", c.getLong(2)).put("end", end).put("allDay", c.getInt(4) == 1))
                }
            }
        } catch (_: Exception) {
        }
        return out.put("perm", true).put("ev", arr).toString()
    }

    @JavascriptInterface
    fun askCalendar() = ui { act.requestCalendar() }

    @JavascriptInterface
    fun openEvent(id: String, begin: String) = ui {
        val i = Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id.toLongOrNull() ?: 0L))
        begin.toDoubleOrNull()?.let { i.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.toLong()) }
        if (!act.start(i)) openCalendar()
    }

    @JavascriptInterface
    fun openCalendar() = ui {
        val sel = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        if (!act.start(sel)) act.start(Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()))
    }

    // ---- now playing: needs the same notification access as the counts ----

    private fun controller(): MediaController? {
        val msm = act.getSystemService(MediaSessionManager::class.java) ?: return null
        val list = msm.getActiveSessions(ComponentName(act, NotifListener::class.java))
        return list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: list.firstOrNull()
    }

    @JavascriptInterface
    fun media(): String {
        val o = JSONObject()
        val c = (try { controller() } catch (e: SecurityException) { return o.put("on", false).toString() } catch (e: Exception) { null })
            ?: return o.put("on", true).put("active", false).toString()
        val md = c.metadata
        val label = try { pm.getApplicationLabel(pm.getApplicationInfo(c.packageName, 0)).toString() } catch (_: Exception) { c.packageName }
        return o.put("on", true).put("active", md != null)
            .put("title", md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "")
            .put("artist", md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: "")
            .put("app", label).put("pkg", c.packageName)
            .put("playing", c.playbackState?.state == PlaybackState.STATE_PLAYING)
            .toString()
    }

    @JavascriptInterface
    fun mediaCmd(cmd: String) = ui {
        try {
            val c = controller() ?: return@ui
            val t = c.transportControls
            when (cmd) {
                "prev" -> t.skipToPrevious()
                "next" -> t.skipToNext()
                "play" -> t.play()
                "pause" -> t.pause()
                else -> if (c.playbackState?.state == PlaybackState.STATE_PLAYING) t.pause() else t.play()
            }
        } catch (_: Exception) {
        }
    }

    // ---- over-the-air UI ----

    @JavascriptInterface
    fun update() = act.checkForUi(manual = true)

    @JavascriptInterface
    fun version(): Int = try { pm.getPackageInfo(act.packageName, 0).versionCode } catch (_: Exception) { 0 }
}
