package snd.komelia.util

// ponytail: see komelia-infra/database/wasm/.../JsUtils.kt for why this is not deduplicated.

fun <T : JsAny> makeJsObject(): T = js("{ return {}; }")

@Suppress("UNUSED_PARAMETER")
private fun getObjectField(obj: JsAny, name: String): JsAny? = js("obj[name]")

@Suppress("UNUSED_PARAMETER")
private fun setObjectField(obj: JsAny, name: String, value: JsAny): Unit = js("obj[name]=value")

operator fun JsAny.get(name: String): JsAny? =
    getObjectField(this, name)

operator fun JsAny.set(name: String, value: JsAny) =
    setObjectField(this, name, value)

operator fun JsAny.set(name: String, value: String) =
    setObjectField(this, name, value.toJsString())

