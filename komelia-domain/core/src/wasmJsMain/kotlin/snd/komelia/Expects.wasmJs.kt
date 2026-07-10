package snd.komelia

import io.ktor.http.*
import org.w3c.dom.url.URL

actual fun codepointsCount(string: String): Long = jsCodepoints(string).toLong()

actual fun Float.formatDecimal(numberOfDecimals: Int) = toFixed(this, numberOfDecimals)

actual fun Double.formatDecimal(numberOfDecimals: Int) = toFixed(this, numberOfDecimals)

actual fun Url.resolve(childUrl: String): Url {
    val jsUrl = URL(childUrl, this.toString())
    return Url(jsUrl.toString())
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsCodepoints(s: String): Int = js("Array.from(s).length")
private fun toFixed(x: Float, num: Int): String = js("x.toFixed(num)")
private fun toFixed(x: Double, num: Int): String = js("x.toFixed(num)")
