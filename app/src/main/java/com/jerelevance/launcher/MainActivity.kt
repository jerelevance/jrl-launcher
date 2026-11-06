package com.jerelevance.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.UserHandle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader

/**
 * The whole launcher is one full-screen WebView running assets/www/index.html.
 * This activity hosts it, registers as HOME, and relays system events to the page:
 *   back, home (pressed while already home), show / hide (resume / pause), apps (package list changed).
 */
class MainActivity : ComponentActivity() {

    companion object {
        const val HOST = "appassets.androidplatform.net"
        const val START = "https://$HOST/live/index.html"
        private const val FIELD_BLUE = 0xFF1722F6.toInt()
    }

    lateinit var web: WebView
        private set
    private var pageReady = false
    private lateinit var rootView: FrameLayout
    private var insetJs = ""
    private val launcherApps by lazy { getSystemService(LauncherApps::class.java) }
    private val calendarPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { emit("calendar") }
    @Volatile private var fetching = false

    private val appsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = emit("apps")
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = emit("apps")
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = emit("apps")
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = emit("apps")
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = emit("apps")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) WebView.setWebContentsDebuggingEnabled(true)
        Live.dropIfStale(this)

        val loader = WebViewAssetLoader.Builder()
            .setDomain(HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/live/", LiveHandler(this))
            .addPathHandler("/icon/", IconHandler(this))
            .build()

        web = WebView(this).apply {
            setBackgroundColor(FIELD_BLUE)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            isHapticFeedbackEnabled = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                textZoom = 100
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = true
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                    loader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (request.url.host == HOST) return false
                    openExternal(request.url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    pageReady = true
                    if (insetJs.isNotEmpty()) view.evaluateJavascript(insetJs, null)
                }
            }
            addJavascriptInterface(Bridge(this@MainActivity), "JRLAndroid")
        }

        val root = FrameLayout(this).apply { setBackgroundColor(FIELD_BLUE) }
        rootView = root
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        // System bars are drawn over the page; their sizes go to CSS as --sat/--sab/--sal/--sar.
        // The keyboard shrinks the WebView from the bottom so the terminal prompt stays visible.
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val lp = web.layoutParams as FrameLayout.LayoutParams
            if (lp.bottomMargin != ime) { lp.bottomMargin = ime; web.layoutParams = lp }
            val d = resources.displayMetrics.density
            val t = bars.top / d; val r = bars.right / d; val l = bars.left / d
            val b = if (ime > 0) 0f else bars.bottom / d
            insetJs = "(function(s){s.setProperty('--sat','${t}px');s.setProperty('--sar','${r}px');s.setProperty('--sab','${b}px');s.setProperty('--sal','${l}px')})(document.documentElement.style)"
            if (pageReady) web.evaluateJavascript(insetJs, null)
            WindowInsetsCompat.CONSUMED
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = emit("back")
        })

        launcherApps.registerCallback(appsCallback)
        web.loadUrl(START)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) emit("home")
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
        web.onResume()
        emit("show")
        if (Live.due(this)) checkForUi(manual = false)
    }

    override fun onPause() {
        emit("hide")
        web.onPause()
        web.pauseTimers()
        super.onPause()
    }

    override fun onDestroy() {
        launcherApps.unregisterCallback(appsCallback)
        web.removeJavascriptInterface("JRLAndroid")
        web.destroy()
        super.onDestroy()
    }

    /** Sends an event into the page: JRLnative.event(name, data). Safe to call from any thread. */
    fun emit(name: String, data: String = "null") {
        if (!::web.isInitialized) return
        web.post {
            if (pageReady) web.evaluateJavascript("window.JRLnative&&JRLnative.event('$name',$data)", null)
        }
    }

    fun launch(key: String, rect: Rect?) {
        val parts = key.split("/", limit = 2)
        if (parts.size < 2) return
        val (pkg, cls) = parts
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(pkg, cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val opts = if (rect != null && rect.width() > 0 && rect.height() > 0)
            ActivityOptions.makeClipRevealAnimation(web, rect.left, rect.top, rect.width(), rect.height()).toBundle() else null
        try {
            startActivity(intent, opts)
        } catch (e: Exception) {
            val fallback = packageManager.getLaunchIntentForPackage(pkg)
            if (fallback != null) {
                try { startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return } catch (_: Exception) {}
            }
            emit("error", "'launch failed: ${pkg.replace("'", "")}'")
        }
    }

    fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            emit("error", "'nothing can open that link'")
        }
    }

    fun start(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) {
        emit("error", "'that screen is not available on this phone'"); false
    }

    fun requestCalendar() = calendarPermission.launch(Manifest.permission.READ_CALENDAR)

    /** Pulls the latest index.html from the repo. The page reloads itself when it's safe (or right away if asked). */
    fun checkForUi(manual: Boolean) {
        if (fetching) return
        fetching = true
        Thread {
            val r = Live.fetch(applicationContext)
            fetching = false
            if (manual || r == "updated") emit("updated", "'$r'")
        }.start()
    }

    /** Wall mode: the window shows the system wallpaper and the page draws over it with a transparent field. */
    fun setWallBehind(on: Boolean) {
        val bg = if (on) Color.TRANSPARENT else FIELD_BLUE
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER) else window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        window.setBackgroundDrawable(ColorDrawable(bg))
        rootView.setBackgroundColor(bg)
        web.setBackgroundColor(bg)
    }

    fun setDarkBarIcons(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }

    fun showKeyboard() {
        web.requestFocus()
        getSystemService(InputMethodManager::class.java)?.showSoftInput(web, InputMethodManager.SHOW_IMPLICIT)
    }
}
