package com.jerelevance.launcher

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Live wallpaper: the jerelevance colour generator.
 * Settings live in SharedPreferences "wall" (written by the launcher's `wall` command):
 *   mode = marathon | drift, amount = 0..3, speed = 0..2, fps = 8..30
 * It draws only while visible, at a low frame rate, from a ~7k-pixel field scaled up and softened.
 */
class JrlWallpaper : WallpaperService() {

    override fun onCreateEngine(): Engine = GenEngine()

    inner class GenEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {
        private val handler = Handler(Looper.getMainLooper())
        private val prefs = getSharedPreferences("wall", MODE_PRIVATE)
        private val gen = ColorGen()
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private var visible = false
        private var sw = 0
        private var sh = 0
        private var w = 0
        private var h = 0
        private var px = IntArray(0)
        private var tmp = IntArray(0)
        private var bmp: Bitmap? = null
        private var last = 0L
        private var gapMs = 55L
        private val tick = Runnable { frame() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            prefs.registerOnSharedPreferenceChangeListener(this)
            load()
        }

        override fun onDestroy() {
            handler.removeCallbacks(tick)
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            bmp?.recycle(); bmp = null
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(p: SharedPreferences?, key: String?) {
            load()
            if (visible) frame()
        }

        private fun load() {
            gen.mode = if (prefs.getString("mode", "marathon") == "drift") "drift" else "marathon"
            gen.amt = (prefs.getString("amount", "1")?.toIntOrNull() ?: 1).coerceIn(0, 3)
            gen.spd = (prefs.getString("speed", "1")?.toIntOrNull() ?: 1).coerceIn(0, 2)
            gapMs = (1000 / (prefs.getString("fps", "18")?.toIntOrNull() ?: 18).coerceIn(8, 30)).toLong()
            gen.resetSlot()
            val cm = ColorMatrix(); cm.setSaturation(if (gen.mode == "marathon") 1.3f else 1.15f)
            paint.colorFilter = ColorMatrixColorFilter(cm)
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            if (v) { last = 0L; frame() } else handler.removeCallbacks(tick)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            sw = width; sh = height
            val aspect = width.toDouble() / max(1, height)
            w = max(8, sqrt(7000 * aspect).roundToInt())
            h = max(8, (7000 / w.toDouble()).roundToInt())
            px = IntArray(w * h); tmp = IntArray(w * h)
            bmp?.recycle(); bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            frame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(tick)
            super.onSurfaceDestroyed(holder)
        }

        private fun frame() {
            handler.removeCallbacks(tick)
            val b = bmp ?: return
            if (!visible || sw == 0) return
            val now = SystemClock.uptimeMillis()
            val dt = if (last == 0L) 16L else min(100L, now - last)
            last = now
            gen.t += dt / 1000.0 * .75
            gen.render(px, w, h)
            // soften like the site's CSS blur (box blur, a few passes ≈ gaussian)
            val passes = if (gen.mode == "marathon") 2 else 3
            repeat(passes) { blurH(); blurV() }
            b.setPixels(px, 0, w, 0, 0, w, h)
            val holder = surfaceHolder
            var c: Canvas? = null
            try {
                c = try { holder.lockHardwareCanvas() } catch (e: Exception) { holder.lockCanvas() }
                c?.drawBitmap(b, Rect(0, 0, w, h), RectF(0f, 0f, sw.toFloat(), sh.toFloat()), paint)
            } catch (_: Exception) {
            } finally {
                if (c != null) try { holder.unlockCanvasAndPost(c) } catch (_: Exception) {}
            }
            handler.postDelayed(tick, gapMs)
        }

        private fun blurH() {
            val r = 1
            for (y in 0 until h) {
                val o = y * w
                for (x in 0 until w) {
                    var rs = 0; var gs = 0; var bs = 0; var n = 0
                    for (k in -r..r) {
                        val xx = (x + k).coerceIn(0, w - 1); val c = px[o + xx]
                        rs += (c shr 16) and 255; gs += (c shr 8) and 255; bs += c and 255; n++
                    }
                    tmp[o + x] = (0xff shl 24) or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
                }
            }
        }

        private fun blurV() {
            val r = 1
            for (x in 0 until w) {
                for (y in 0 until h) {
                    var rs = 0; var gs = 0; var bs = 0; var n = 0
                    for (k in -r..r) {
                        val yy = (y + k).coerceIn(0, h - 1); val c = tmp[yy * w + x]
                        rs += (c shr 16) and 255; gs += (c shr 8) and 255; bs += c and 255; n++
                    }
                    px[y * w + x] = (0xff shl 24) or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
                }
            }
        }
    }
}
