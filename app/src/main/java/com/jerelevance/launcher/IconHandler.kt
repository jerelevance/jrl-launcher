package com.jerelevance.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.webkit.WebResourceResponse
import androidx.core.graphics.drawable.toBitmap
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Serves app icons to the page at https://appassets.androidplatform.net/icon/<pkg>/<activity>
 * as PNGs. Same origin as the page, so the page can read the pixels and halftone them.
 */
class IconHandler(private val ctx: Context) : WebViewAssetLoader.PathHandler {

    private val cache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }

    override fun handle(path: String): WebResourceResponse? {
        val key = Uri.decode(path).trimStart('/')
        val bytes = cache.get(key) ?: render(key)?.also { cache.put(key, it) } ?: return null
        return WebResourceResponse("image/png", null, ByteArrayInputStream(bytes))
    }

    private fun render(key: String): ByteArray? = try {
        val parts = key.split("/", limit = 2)
        val pm = ctx.packageManager
        val drawable = if (parts.size == 2) pm.getActivityIcon(ComponentName(parts[0], parts[1]))
                       else pm.getApplicationIcon(parts[0])
        val bmp = drawable.toBitmap(144, 144, Bitmap.Config.ARGB_8888)
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}
