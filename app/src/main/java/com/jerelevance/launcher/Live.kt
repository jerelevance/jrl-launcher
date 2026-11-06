package com.jerelevance.launcher

import android.content.Context
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Over-the-air UI. The page lives in one file (index.html). The app pulls the newest copy from the repo's
 * main branch in the background and keeps it in private storage; next time the page loads, that copy is used.
 * Kotlin changes still need a new APK; look-and-behaviour changes don't.
 */
object Live {
    const val SRC = "https://raw.githubusercontent.com/jerelevance/jrl-launcher/main/app/src/main/assets/www/index.html"
    private const val MIN_GAP_MS = 30 * 60 * 1000L

    fun file(ctx: Context) = File(ctx.filesDir, "live/index.html")

    /** A freshly installed APK always wins over an older downloaded page. */
    fun dropIfStale(ctx: Context) {
        val f = file(ctx)
        if (!f.exists()) return
        val installed = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime } catch (_: Exception) { 0L }
        if (installed > f.lastModified()) f.delete()
    }

    fun due(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences("live", Context.MODE_PRIVATE)
        return System.currentTimeMillis() - p.getLong("checked", 0L) > MIN_GAP_MS
    }

    /** Returns "updated", "same" or "failed". Blocking; call off the main thread. */
    fun fetch(ctx: Context): String {
        ctx.getSharedPreferences("live", Context.MODE_PRIVATE).edit().putLong("checked", System.currentTimeMillis()).apply()
        return try {
            val c = (URL(SRC + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 15000; useCaches = false
                setRequestProperty("User-Agent", "jrl-launcher")
            }
            if (c.responseCode != 200) return "failed"
            val body = c.inputStream.use { it.readBytes() }
            val text = String(body, Charsets.UTF_8)
            if (body.size < 20000 || !text.contains("JRLnative")) return "failed"
            val current = file(ctx).takeIf { it.exists() }?.readBytes()
                ?: ctx.assets.open("www/index.html").use { it.readBytes() }
            if (current.contentEquals(body)) return "same"
            val f = file(ctx); f.parentFile?.mkdirs()
            val tmp = File(f.parentFile, "index.tmp"); tmp.writeBytes(body)
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
            "updated"
        } catch (e: Exception) {
            "failed"
        }
    }
}

/** Serves /live/…: the downloaded index.html when there is one, otherwise the bundled assets (fonts always come from assets). */
class LiveHandler(private val ctx: Context) : WebViewAssetLoader.PathHandler {
    override fun handle(path: String): WebResourceResponse? {
        val p = path.trimStart('/').ifEmpty { "index.html" }
        if (p.contains("..")) return null
        val mime = when (p.substringAfterLast('.', "")) {
            "html" -> "text/html"; "ttf" -> "font/ttf"; "js" -> "text/javascript"; "css" -> "text/css"; "png" -> "image/png"
            else -> "application/octet-stream"
        }
        return try {
            val live = Live.file(ctx)
            val stream = if (p == "index.html" && live.exists()) FileInputStream(live) else ctx.assets.open("www/$p")
            WebResourceResponse(mime, if (mime.startsWith("text")) "utf-8" else null, stream)
        } catch (e: Exception) {
            null
        }
    }
}
