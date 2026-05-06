package me.apomazkin.settingstab

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.viewinterop.AndroidView
import me.apomazkin.settingstab.LogTags
import me.apomazkin.ui.logger.LexemeLogger

private const val ALLOWED_DOMAIN = "kilgoret.github.io"

@Composable
fun WebViewScreen(
    url: String,
    title: String,
    pageKey: String,
    logger: LexemeLogger,
    onBackPress: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    BackHandler {
        logger.log(tag = LogTags.WEBVIEW, message = "back: $pageKey")
        onBackPress()
    }

    Scaffold(
        topBar = {
            WebViewAppBar(
                title = title,
                onBackPress = {
                    logger.log(tag = LogTags.WEBVIEW, message = "back: $pageKey")
                    onBackPress()
                },
            )
        },
    ) { paddings: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (isError) {
                ErrorContent(
                    onRetry = {
                        logger.log(tag = LogTags.WEBVIEW, message = "retry: $url")
                        isError = false
                        isLoading = true
                    }
                )
            } else {
                val context = LocalContext.current
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { webView ->
                        logger.log(tag = LogTags.WEBVIEW, message = "destroy: $url")
                        webView.stopLoading()
                        webView.destroy()
                    },
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.allowFileAccess = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    loadingUrl: String?,
                                    favicon: Bitmap?
                                ) {
                                    logger.log(tag = LogTags.WEBVIEW, message = "loading: $loadingUrl")
                                    isLoading = true
                                    isError = false
                                }

                                override fun onPageFinished(
                                    view: WebView?,
                                    finishedUrl: String?
                                ) {
                                    logger.log(tag = LogTags.WEBVIEW, message = "loaded: $finishedUrl")
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        logger.log(tag = LogTags.WEBVIEW, message = "error: $url")
                                        isLoading = false
                                        isError = true
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: android.webkit.WebResourceResponse?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        logger.log(tag = LogTags.WEBVIEW, message = "http error: ${errorResponse?.statusCode} $url")
                                        isLoading = false
                                        isError = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val requestUrl = request?.url?.toString() ?: return false
                                    val host = Uri.parse(requestUrl).host ?: return false
                                    return if (host == ALLOWED_DOMAIN) {
                                        false
                                    } else {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                            )
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, requestUrl, Toast.LENGTH_SHORT).show()
                                        }
                                        true
                                    }
                                }
                            }
                            loadUrl(url)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = me.apomazkin.core_resources.R.string.webview_error_title),
            color = androidx.compose.ui.graphics.Color.Black,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(
                text = stringResource(id = me.apomazkin.core_resources.R.string.webview_retry),
            )
        }
    }
}
