package com.example.ui.webview

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Check if WebView is supported on this device/emulator without throwing a fatal exception
    val isWebViewSupported = remember(context) {
        try {
            android.webkit.WebSettings.getDefaultUserAgent(context)
            true
        } catch (t: Throwable) {
            false
        }
    }

    var hasWebViewError by remember { mutableStateOf(false) }

    if (!isWebViewSupported || hasWebViewError) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "Vizualizarea web nu este disponibilă pe acest dispozitiv.\nPuteți accesa linkul direct:\n$url",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
        return
    }

    var isLoading by remember { mutableStateOf(true) }
    var lastRequestedUrl by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { factoryContext ->
                WebView(factoryContext).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    
                    // Enable scrollbars
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = true

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: android.webkit.GeolocationPermissions.Callback?
                        ) {
                            try {
                                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                                    factoryContext,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                                    factoryContext,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (hasFine || hasCoarse) {
                                    callback?.invoke(origin, true, false)
                                } else {
                                    callback?.invoke(origin, false, false)
                                }
                            } catch (e: Exception) {
                                callback?.invoke(origin, false, false)
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        // Pre-compiled list of common ad, tracker, and telemetry domain keywords for high-efficiency blocking.
                        private val adKeywords = setOf(
                            "googleads", "doubleclick", "pagead", "googlesyndication",
                            "adnxs", "criteo", "rtbhouse", "outbrain", "taboola", 
                            "adform", "scorecardresearch", "quantserve", "analytics", 
                            "google-analytics", "popads", "mgid", "revcontent",
                            "adservice", "adsystem", "amazon-adsystem", "assoc-amazon",
                            "adserver", "adsbygoogle", "smartadserver", "pubmatic",
                            "rubiconproject", "openx", "indexww", "casalemedia",
                            "sharethrough", "yieldmo", "triplelift", "teads", "media.net",
                            "bidswitch", "sovrn", "adtech", "serving-sys", "gumgum"
                        )

                        private fun isAdRequest(requestUrl: String): Boolean {
                            return try {
                                val host = android.net.Uri.parse(requestUrl).host?.lowercase() ?: return false
                                adKeywords.any { host.contains(it) }
                            } catch (e: Exception) {
                                false
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            try {
                                if (request != null) {
                                    val urlString = request.url.toString()
                                    if (isAdRequest(urlString)) {
                                        return WebResourceResponse(
                                            "text/plain", 
                                            "UTF-8", 
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                // Ignored
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            try {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            try {
                                super.onPageFinished(view, url)
                                isLoading = false
                                
                                val cssHelperJs = """
                                    (function() {
                                        var css = 'iframe[src*="googleads"], div[class*="ad-"], div[class*="-ad-"], div[id*="ad-"], div[id*="-ad-"], ins.adsbygoogle, .advertisement, .ad-banner, .adsbox, .ad-container, [id^="ad_"], [class^="ad_"] { display: none !important; height: 0px !important; min-height: 0px !important; visibility: hidden !important; opacity: 0 !important; }';
                                        var head = document.head || document.getElementsByTagName('head')[0];
                                        if (head) {
                                            var style = document.createElement('style');
                                            style.type = 'text/css';
                                            style.appendChild(document.createTextNode(css));
                                            head.appendChild(style);
                                        }
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(cssHelperJs, null)
                            } catch (e: Exception) {
                                isLoading = false
                            }
                        }
                        
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            try {
                                super.onReceivedError(view, request, error)
                                isLoading = false
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            try {
                                handler?.proceed()
                            } catch (e: Exception) {
                                // Ignored
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            try {
                                hasWebViewError = true
                                (view?.parent as? android.view.ViewGroup)?.removeView(view)
                                view?.destroy()
                            } catch (e: Exception) {
                                // Ignored
                            }
                            return true
                        }
                    }
                }
            },
            onRelease = { webView ->
                try {
                    webView.stopLoading()
                } catch (e: Exception) {
                    // Prevent any crash during disposal
                }
            },
            update = { webView ->
                try {
                    if (lastRequestedUrl != url) {
                        lastRequestedUrl = url
                        webView.loadUrl(url)
                    }
                } catch (e: Exception) {
                    // Prevent crash during update
                }
            }
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}
