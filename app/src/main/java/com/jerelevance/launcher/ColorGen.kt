package com.jerelevance.launcher

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The jerelevance colour generator (template.html), ported line for line.
 * drift    = four bright colours turning slowly around the wheel
 * marathon = hard, flat, saturated colour plus pure black and white; no greys, no blending through grey
 * Renders a small ARGB field; the wallpaper scales and softens it.
 */
class ColorGen {
    class Col(val L: Double, val C: Double, val h: Double)

    var mode = "marathon"
    var amt = 1          // 0..3: how hard and how fast the Marathon sets cut
    var spd = 1          // 0 slow, 1 normal, 2 fast
    var t = Math.random() * 1000
    private val seed = Math.random() * 360

    private val MARA = arrayOf(
        doubleArrayOf(.93, .23, 125.0), doubleArrayOf(.65, .27, 0.0), doubleArrayOf(.45, .29, 266.0), doubleArrayOf(.7, .2, 45.0),
        doubleArrayOf(.6, .25, 28.0), doubleArrayOf(.88, .13, 170.0), doubleArrayOf(.93, .21, 108.0), doubleArrayOf(.52, .26, 295.0),
        doubleArrayOf(1.0, 0.0, 0.0), doubleArrayOf(0.0, 0.0, 0.0)
    )
    private val AMT = arrayOf(doubleArrayOf(.16, 14.0, 1.3), doubleArrayOf(.3, 11.0, 1.6), doubleArrayOf(.62, 7.0, 2.4), doubleArrayOf(.95, 5.0, 3.0))
    private val SPD = doubleArrayOf(1.9, 1.0, .45)

    private var mSlot = Long.MIN_VALUE
    private var mPal: Array<Array<Col>> = arrayOf()
    private val lut = IntArray(256)

    fun resetSlot() { mSlot = Long.MIN_VALUE }

    /* mulberry32, seeded per slot, same as the site */
    private class Rng(n: Long, seed: Double) {
        private var s: Int = ((n * 2654435761L + (seed * 1000).toLong()) and 0xffffffffL).toInt()
        fun next(): Double {
            s += 0x6D2B79F5
            var x = (s xor (s ushr 15)) * (1 or s)
            x = (x + (x xor (x ushr 7)) * (61 or x)) xor x
            return ((x xor (x ushr 14)).toLong() and 0xffffffffL) / 4294967296.0
        }
    }

    /* mix around the hue wheel; black and white borrow the other colour's hue so nothing passes through grey */
    private fun mixC(a: Col, b: Col, f: Double): Col {
        val ha = if (a.C < .01) b.h else a.h
        val hb = if (b.C < .01) a.h else b.h
        val dh = ((hb - ha + 540) % 360) - 180
        val L = a.L + (b.L - a.L) * f
        var C = a.C + (b.C - a.C) * f
        if (a.C >= .01 && b.C >= .01) C = max(.12, C)
        return Col(L, C, (ha + dh * f + 360) % 360)
    }

    private fun maraSet(n: Long): Array<Col> {
        val r = Rng(n, seed)
        val pool = MARA.copyOfRange(0, 8).toMutableList()
        var pick = mutableListOf<DoubleArray>()
        while (pick.size < 4) pick.add(pool.removeAt(floor(r.next() * pool.size).toInt()))
        pick.sortBy { it[0] }
        pick = mutableListOf(pick[0], pick[3], pick[1], pick[2])
        val bw = r.next()
        if (bw < .35) pick[0] = MARA[9] else if (bw < .7) pick[3] = MARA[8] else if (bw < .9) { pick[0] = MARA[9]; pick[3] = MARA[8] }
        return Array(4) { Col(pick[it][0], pick[it][1], pick[it][2]) }
    }

    private fun maraPalette(time: Double): Array<Col> {
        val span = AMT[amt][1] * SPD[spd]
        val u = time / span
        val n = floor(u).toLong()
        var f = u - n
        if (n != mSlot) { mSlot = n; mPal = arrayOf(maraSet(n), maraSet(n + 1)) }
        f = if (f < .35) 0.0 else (f - .35) / .65
        f = f * f * (3 - 2 * f)
        return Array(4) { i ->
            val c = mixC(mPal[0][i], mPal[1][i], f)
            val L = c.L + (if (c.C > .01) .03 * sin(time * (.7 + i * .13) + i * 1.7) else 0.0)
            Col(L.coerceIn(0.0, 1.0), c.C, c.h)
        }
    }

    private fun palette(time: Double): Array<Col> {
        val base = (seed + time * 10) % 360
        val offs = doubleArrayOf(0.0, 28 + 10 * sin(time * .5), 62 + 14 * sin(time * .33), 100 + 16 * cos(time * .41))
        val Ls = doubleArrayOf(.5, .64, .56, .7)
        return Array(4) { i ->
            Col(Ls[i] + .05 * sin(time * (.7 + i * .13) + i * 1.7), .17 + .04 * sin(time * (.55 + i * .11) + i * 2.3), (base + offs[i] + 360) % 360)
        }
    }

    private fun oklch(L: Double, C: Double, h: Double): Int {
        val a = C * cos(h * PI / 180); val b = C * sin(h * PI / 180)
        val l = (L + .3963377774 * a + .2158037573 * b).pow(3)
        val m = (L - .1055613458 * a - .0638541728 * b).pow(3)
        val s = (L - .0894841775 * a - 1.2914855480 * b).pow(3)
        fun g(v0: Double): Int {
            val v = if (v0 <= .0031308) 12.92 * v0 else 1.055 * v0.pow(1 / 2.4) - .055
            return (v.coerceIn(0.0, 1.0) * 255).roundToInt()
        }
        val r = g(4.0767416621 * l - 3.3077115913 * m + .2309699292 * s)
        val gg = g(-1.2684380046 * l + 2.6097574011 * m - .3413193965 * s)
        val bb = g(-.0041960863 * l - .7034186147 * m + 1.7076147010 * s)
        return (0xff shl 24) or (r shl 16) or (gg shl 8) or bb
    }

    private fun ramp(p: Array<Col>) {
        val stops = arrayOf(p[0], p[1], p[2], p[3], p[2], p[1], p[0])
        val mara = mode == "marathon"
        for (k in 0 until 256) {
            val u = k / 256.0 * 6
            val i = floor(u).toInt()
            var f = u - i
            val a = stops[i]; val b = stops[i + 1]
            f = f * f * (3 - 2 * f)
            if (mara) {
                val e = if (a.C < .01 || b.C < .01) 3.0 + amt else AMT[amt][2]
                f = if (f < .5) .5 * (2 * f).pow(e) else 1 - .5 * (2 - 2 * f).pow(e)
            }
            val c = mixC(a, b, f)
            lut[k] = oklch(c.L, c.C, c.h)
        }
    }

    /** Fills px (w*h ARGB) with the field at the current time. */
    fun render(px: IntArray, w: Int, h: Int) {
        val mara = mode == "marathon"
        ramp(if (mara) maraPalette(t) else palette(t))
        val T = t * .9
        val u = .9 / min(w, h)
        val k1 = if (mara) AMT[amt][0] else .36
        val drift = .32 * sin(T * .23)
        for (y in 0 until h) {
            val ny = 1 + (y - h / 2.0) * u
            val sy1 = .8 * sin(ny * 1.7 + T * 1.1) + .3 * sin(ny * 3.1 - T * .7)
            for (x in 0 until w) {
                val nx = .85 + (x - w / 2.0) * u
                val qx = nx + sy1
                val qy = ny + .8 * cos(nx * 1.5 - T * .9) + .3 * cos(nx * 2.9 + T * .6)
                val rx = qx + .6 * sin(qy * 2.3 + T * .8)
                val ry = qy + .6 * cos(qx * 2.1 - T * .75)
                var v = .5 + .5 * sin(rx * 1.3 + ry * 1.1 + T * .5)
                v = min(.999, max(0.0, .5 + (v - .5) * k1 + .06 * sin(rx * .7 - ry * .9 + T * .3) + drift))
                px[y * w + x] = lut[(v * 255).toInt()]
            }
        }
    }
}
