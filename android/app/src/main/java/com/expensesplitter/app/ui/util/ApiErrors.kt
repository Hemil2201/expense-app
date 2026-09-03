package com.expensesplitter.app.ui.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/** FastAPI error responses are `{"detail": "..."}` — pulls that message out
 * instead of showing the raw JSON body or a bare "HTTP 401" status line. */
fun HttpException.detailMessage(): String {
    val body = response()?.errorBody()?.string()
    val parsed = body?.let {
        runCatching { Json.parseToJsonElement(it).jsonObject["detail"]?.jsonPrimitive?.content }.getOrNull()
    }
    return parsed ?: message()
}
