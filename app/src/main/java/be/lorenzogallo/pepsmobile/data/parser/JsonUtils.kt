package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.ParseException
import be.lorenzogallo.pepsmobile.data.model.SessionExpiredException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

internal val LenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun parseAjaxObject(body: String): JsonObject {
    val trimmed = body.trim()
    if (!trimmed.startsWith("{")) {
        if (trimmed.startsWith("<") || HtmlUtils.isLoginPage(trimmed)) throw SessionExpiredException()
        throw ParseException("Réponse inattendue du serveur")
    }
    val element = try {
        LenientJson.parseToJsonElement(trimmed)
    } catch (e: SerializationException) {
        throw ParseException("JSON invalide", e)
    }
    val obj = element as? JsonObject ?: throw ParseException("Objet JSON attendu")
    if (obj.bool("error")) {
        val message = obj.str("message")
        if (message.contains("connect", ignoreCase = true) || message.contains("session", ignoreCase = true)) {
            throw SessionExpiredException()
        }
        throw ParseException(message.ifBlank { "Erreur renvoyée par PepSchool" })
    }
    return obj
}

internal fun JsonObject.str(key: String): String {
    val p = this[key] as? JsonPrimitive ?: return ""
    if (p is JsonNull) return ""
    return p.content
}

internal fun JsonObject.long(key: String): Long? {
    val p = this[key] as? JsonPrimitive ?: return null
    if (p is JsonNull) return null
    return p.longOrNull ?: p.content.toLongOrNull()
}

internal fun JsonObject.int(key: String): Int? {
    val p = this[key] as? JsonPrimitive ?: return null
    if (p is JsonNull) return null
    return p.intOrNull ?: p.content.toIntOrNull()
}

internal fun JsonObject.bool(key: String): Boolean = boolish(this[key])

internal fun boolish(element: JsonElement?): Boolean {
    val p = element as? JsonPrimitive ?: return false
    if (p is JsonNull) return false
    p.booleanOrNull?.let { return it }
    p.intOrNull?.let { return it != 0 }
    return p.content.equals("true", ignoreCase = true) || p.content == "1"
}
