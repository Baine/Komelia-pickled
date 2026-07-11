package snd.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebMessagePort.WebMessageCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.DataNode
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.parseInputStream
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import snd.webview.WebviewCallback.CallbackResponse
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.reflect.typeOf


val logger = KotlinLogging.logger {}

@SuppressLint("SetJavaScriptEnabled")
actual class KomeliaWebview(private val webview: WebView) : WebViewClient(), AutoCloseable {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    val mainDispatcherScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val isRunning = AtomicBoolean(false)
    private var currentUrl: Uri? = null
    private val bindFunctions = mutableMapOf<String, WebviewCallback>()
    private var interceptor: RequestInterceptor? = null
    private var incomingPort: WebMessagePort? = null
    private var outgoingPort: WebMessagePort? = null

    init {
        webview.settings.javaScriptEnabled = true
        webview.settings.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
        webview.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        webview.settings.setSupportZoom(true)
        webview.webViewClient = this

        //TODO look into WebViewCompat.addWebMessageListener() and WebViewCompat.addDocumentStartJavaScript()
        // should be possible to inject js without modifying original html document
//        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
//            WebViewCompat.addDocumentStartJavaScript(webview, initScript, setOf("*"))
//        }
    }

    actual suspend inline fun <reified JsArgs, reified Result> bind(
        name: String,
        function: JsCallback<JsArgs, Result>
    ) {
        bind(name) { id, jsRequest ->
            mainDispatcherScope.launch {
                runCatching {
                    val argsClass = typeOf<JsArgs>().classifier as KClass<*>
                    val resultClass = typeOf<Result>().classifier as KClass<*>

                    val arguments = json.decodeFromString<List<JsArgs>>(jsRequest)
                    val argument = if (argsClass == Unit::class) {
                        Unit as JsArgs
                    } else {
                        arguments[0]
                    }

                    val result = function.run(argument)

                    if (resultClass == Unit::class) {
                        bindReturn(id, json.encodeToString(CallbackResponse("")))
                    } else {
                        val json = json.encodeToString<CallbackResponse<Result>>(CallbackResponse(result))
                        bindReturn(id, json)
                    }
                }.onFailure { error ->
                    logger.error(error) { "Encountered error during execution of bind function \"$name\"; js params: $jsRequest" }
                    val message = json.encodeToString<CallbackResponse<String>>(
                        CallbackResponse(error.message ?: error.stackTraceToString())
                    )
                    bindReject(id, message)
                }
            }
        }
    }

    actual fun navigate(uri: String) {
        mainDispatcherScope.launch {
            currentUrl = Uri.parse(uri)
            if (isRunning.get()) {
                webview.loadUrl(uri)
            }
        }
    }

    actual fun registerRequestInterceptor(handler: RequestInterceptor) {
        mainDispatcherScope.launch {
            interceptor = handler
        }
    }

    actual fun start() {
        mainDispatcherScope.launch {
            if (!isRunning.compareAndSet(false, true)) return@launch
            currentUrl?.let {
                webview.loadUrl(it.toString())
            }
        }
    }

    override fun close() {}

    suspend fun bind(name: String, callback: WebviewCallback) {
        withContext(Dispatchers.Main) {
            bindFunctions[name] = callback

            webview.evaluateJavascript(
                """
                  if (window.__webview__) {
                    window.__webview__.onBind(${json.encodeToString(name)});
                  }
                """,
                null
            )
        }
    }

    private suspend fun resolve(id: String, status: Int, result: String) {
        withContext(Dispatchers.Main) {
            webview.evaluateJavascript(
                """
              window.__webview__.onReply(
                ${json.encodeToString(id)}, $status, ${json.encodeToString(result)}
              )
            """,
                null
            )
        }
    }

    suspend fun bindReturn(id: String, result: String) {
        resolve(id, 0, result)
    }

    suspend fun bindReject(id: String, message: String) {
        resolve(id, -1, message)
    }

    // TODO non blocking
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val response = runBlocking { interceptor?.run(request = request.toResourceRequest()) } ?: return null

        if (currentUrl == request.url) {
            val htmlDocument = Ksoup.parseInputStream(response.data.inputStream(), "")

            val bindScriptElement = Element("script")
            bindScriptElement.appendChild(DataNode(createBindScript()))
            htmlDocument.head().prependChild(bindScriptElement)

            val initScriptElement = Element("script")
            initScriptElement.appendChild(DataNode(initScript))
            htmlDocument.head().prependChild(initScriptElement)

            return WebResourceResponse(
                "text/html",
                "utf-8",
                htmlDocument.outerHtml().byteInputStream(Charsets.UTF_8)
            )
        }

        return WebResourceResponse(response.contentType, null, response.data.inputStream())
    }

    private fun WebResourceRequest.toResourceRequest() = ResourceRequest(
        url = Url(this.url.toString()),
        method = HttpMethod.parse(this.method),
        requestHeaders = HeadersBuilder()
            .apply { this@toResourceRequest.requestHeaders.forEach { (k, v) -> append(k, v) } }
            .build()
    )

    override fun onPageFinished(view: WebView, url: String) {
        val channel = view.createWebMessageChannel()
        this.incomingPort = channel[0]
        this.outgoingPort = channel[1]
        channel[0].setWebMessageCallback(object : WebMessageCallback() {
            override fun onMessage(port: WebMessagePort, message: WebMessage) {
                val webMessage = json.decodeFromString<WebviewMessage>(message.data)
                val callback = bindFunctions[webMessage.method]
                if (callback == null) {
                    mainDispatcherScope.launch {
                        bindReject(webMessage.id, "Function not found")
                    }
                    return
                }
                callback.run(webMessage.id, webMessage.params.toString())
            }
        })
        view.postWebMessage(WebMessage("", arrayOf(outgoingPort)), requireNotNull(currentUrl))
    }


    private fun createBindScript(): String {
        val jsNames = json.encodeToString(bindFunctions.keys)
        return """
                'use strict';
                var methods = $jsNames;
                methods.forEach(function(name){
                  window.__webview__.onBind(name);
                })
            """
    }
}

actual fun webviewIsAvailable() = true
