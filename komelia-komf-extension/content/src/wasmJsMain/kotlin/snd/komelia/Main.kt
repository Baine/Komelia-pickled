@file:OptIn(InternalAPI::class)

package snd.komelia

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import snd.komelia.db.SettingsStateWrapper
import snd.komelia.db.repository.KomfSettingsRepositoryWrapper
import snd.komelia.db.settings.LocalStorageSettingsRepository
import snd.komf.client.KomfClientFactory
import kotlin.coroutines.CoroutineContext


val logger = KotlinLogging.logger("Komf")

fun main() {
    patchPromiseRealm()
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    coroutineScope.launch {
        val app = initApplication(coroutineScope)
        app.launch()
    }
}

private suspend fun initApplication(coroutineScope: CoroutineScope): AppState {
    val localStorageRepository = LocalStorageSettingsRepository()
    val komfSettingsRepository = KomfSettingsRepositoryWrapper(
        SettingsStateWrapper(
            localStorageRepository.getKomfSettings(),
            localStorageRepository::saveKomfSettings
        )
    )
    val komfUrl = komfSettingsRepository.getKomfUrl().stateIn(coroutineScope)
    val komfClientFactory = KomfClientFactory(
        ktor = createKtorClient(),
        // the repo default remoteUrl is window.location.href — right for the komf-served
        // webui, but here window.location is the Kavita/Komga page, so a blank or
        // page-origin URL means "never configured". Fail every request with an actionable
        // message (shown verbatim by AppNotifications) instead of hitting the media server.
        baseUrl = {
            val url = komfUrl.value
            val origin = window.location.origin
            if (url.isBlank() || url == origin || url.startsWith("$origin/") ||
                url.startsWith("$origin?") || url.startsWith("$origin#")
            ) {
                error("Komf server URL is not configured — set it in the extension settings (Connection tab)")
            }
            url
        },
    )
    val vmFactory = KomfViewModelFactory(
        komfClientFactory = komfClientFactory,
        appNotifications = AppNotifications(),
        settingsRepository = komfSettingsRepository
    )

    return AppState(vmFactory)

}

private val sseRequestAttr = AttributeKey<Boolean>("SSERequestFlag")

// Firefox content scripts run against the page realm: every Promise produced by
// fetch/Response.blob/json/text/arrayBuffer/ReadableStreamReader.read is a PAGE-realm
// promise, which fails Kotlin/Wasm's injected `instanceof Promise` cast check
// ("Cannot cast instance of Promise to Promise: incompatible types") in ktor, coil and
// compose glue alike (all raw in the shipped bundle - only snd.komelia's own @JsFuns were
// wrapped). Re-wrap every entry point with an extension-realm Promise.resolve():
// functionally transparent, same-realm no-op on Chrome, fixes all sites at once.
@JsFun(
    """() => {
    const g = globalThis;
    if (g.__komeliaPromiseRealmPatch) return;
    g.__komeliaPromiseRealmPatch = true;
    const wrap = (o, m) => {
        const f = o[m];
        if (typeof f !== "function") return;
        o[m] = function (...a) { return Promise.resolve(f.apply(this, a)); };
    };
    wrap(g, "fetch");
    if (g.Response) for (const m of ["blob", "json", "text", "arrayBuffer"]) wrap(g.Response.prototype, m);
    if (g.Blob) for (const m of ["arrayBuffer", "text", "stream"]) wrap(g.Blob.prototype, m);
    if (g.ReadableStream) {
        const p = g.ReadableStream.prototype, gr = p.getReader;
        if (typeof gr === "function") p.getReader = function (...a) {
            const r = gr.apply(this, a);
            if (r) { wrap(r, "read"); wrap(r, "cancel"); }
            return r;
        };
    }
}"""
)
private external fun patchPromiseRealm()

private val CustomResponse: AttributeKey<Any> = AttributeKey("CustomResponse")

// firefox returns empty list when calling Array.from(headers.keys()) on fetch response
// this results in missing content-type header and breaks json decode
// https://github.com/ktorio/ktor/blob/2673d3915e6d78ff8894ca2f3f2e34b03a73c9f2/ktor-client/ktor-client-core/wasmJs/src/io/ktor/client/engine/js/WasmJsClientEngine.kt#L161
// use this hack to inject our own content-type header into every response
private fun createKtorClient(): HttpClient {
    return HttpClient(Js) {
        install("ContentTypeHack") {
            receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
                if (!response.headers.isEmpty()) {
                    proceed()
                    return@intercept
                }

                val request = response.request
                val isSse = request.attributes.getOrNull(sseRequestAttr) == true

                val newResponse = object : HttpResponse() {
                    override val call: HttpClientCall = response.call
                    override val status: HttpStatusCode = response.status
                    override val version: HttpProtocolVersion = response.version
                    override val requestTime: GMTDate = response.requestTime
                    override val responseTime: GMTDate = response.responseTime
                    override val rawContent: ByteReadChannel = if (isSse) ByteReadChannel.Empty else response.rawContent

                    override val coroutineContext: CoroutineContext = response.coroutineContext
                    override val headers: Headers = HeadersBuilder().apply {
                        appendAll(response.headers)

                        if (isSse) {
                            this.append(HttpHeaders.ContentType, "text/event-stream")
                            this.append(HttpHeaders.TransferEncoding, "chunked")
                        } else {
                            this.append(HttpHeaders.ContentType, "application/json")
                        }
                    }.build()
                }
                if (isSse) {
                    request.attributes.remove(CustomResponse)
                    request.attributes.put(
                        CustomResponse,
                        DefaultClientSSESession(request.content as SSEClientContent, response.rawContent)
                    )
                }

                this.proceedWith(newResponse)
            }
        }
    }
}

