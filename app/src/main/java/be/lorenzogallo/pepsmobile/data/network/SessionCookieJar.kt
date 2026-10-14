package be.lorenzogallo.pepsmobile.data.network

import be.lorenzogallo.pepsmobile.data.security.SecureStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar(
    private val host: String,
    private val store: SecureStore,
) : CookieJar {
    private val lock = Any()
    private val cookies = LinkedHashMap<String, String>()

    init {
        load()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!url.host.equals(host, ignoreCase = true)) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            for (c in cookies) {
                val explicitlyDeleted = c.value.isEmpty() || c.value == "deleted" || c.expiresAt < now - EXPIRY_GRACE_MS
                if (explicitlyDeleted) this.cookies.remove(c.name) else this.cookies[c.name] = c.value
            }
            persistLocked()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.host.equals(host, ignoreCase = true)) return emptyList()
        return synchronized(lock) {
            cookies.map { (name, value) ->
                Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(host)
                    .path("/")
                    .secure()
                    .build()
            }
        }
    }

    operator fun get(name: String): String? = synchronized(lock) { cookies[name] }

    fun snapshot(): Map<String, String> = synchronized(lock) { LinkedHashMap(cookies) }

    fun clear() {
        synchronized(lock) {
            cookies.clear()
            store.remove(SecureStore.KEY_COOKIES)
        }
    }

    private fun persistLocked() {
        val json = JsonObject(cookies.mapValues { JsonPrimitive(it.value) }).toString()
        store.put(SecureStore.KEY_COOKIES, json)
    }

    private fun load() {
        val raw = store.get(SecureStore.KEY_COOKIES) ?: return
        runCatching {
            Json.parseToJsonElement(raw).jsonObject.forEach { (k, v) ->
                val value = v.jsonPrimitive.content
                if (value.isNotEmpty()) cookies[k] = value
            }
        }.onFailure { store.remove(SecureStore.KEY_COOKIES) }
    }

    private companion object {
        const val EXPIRY_GRACE_MS = 24L * 60 * 60 * 1000
    }
}
