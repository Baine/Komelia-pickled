package snd.komelia.db.color

import com.juul.indexeddb.Key
import kotlin.js.toJsString

// ponytail: adapter for com.juul.indexeddb:core API; Key() expects JsAny, so convert String first
fun IDBKey(value: String): Key = Key(value.toJsString())
