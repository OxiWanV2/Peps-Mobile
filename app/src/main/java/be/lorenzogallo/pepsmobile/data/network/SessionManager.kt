package be.lorenzogallo.pepsmobile.data.network

import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.NetworkException
import be.lorenzogallo.pepsmobile.data.model.SessionExpiredException
import be.lorenzogallo.pepsmobile.data.parser.HtmlUtils
import be.lorenzogallo.pepsmobile.data.security.SecureStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SessionManager(
    private val api: PepsApi,
    private val cookieJar: SessionCookieJar,
    private val store: SecureStore,
) {
    private val mutex = Mutex()

    @Volatile
    private var generation = 0L

    val hasToken: Boolean get() = cookieJar["token"] != null

    suspend fun login(email: String, password: String) {
        mutex.withLock {
            performLogin(email, password)
            store.put(SecureStore.KEY_EMAIL, email)
            store.put(SecureStore.KEY_PASSWORD, password)
        }
    }

    private suspend fun ensureSession(): Long = mutex.withLock {
        val lastAuth = store.get(SecureStore.KEY_LAST_AUTH)?.toLongOrNull() ?: 0L
        val stale = System.currentTimeMillis() - lastAuth >= REAUTH_INTERVAL_MS
        if (!hasToken || stale) reauthenticateLocked()
        generation
    }

    private suspend fun reauthenticate(seenGeneration: Long) = mutex.withLock {
        if (generation == seenGeneration) reauthenticateLocked()
    }

    private suspend fun reauthenticateLocked() {
        val email = store.get(SecureStore.KEY_EMAIL)
        val password = store.get(SecureStore.KEY_PASSWORD)
        if (email.isNullOrBlank() || password.isNullOrEmpty()) {
            throw AuthException("Aucun identifiant enregistré, reconnexion nécessaire")
        }
        performLogin(email, password)
    }

    private suspend fun performLogin(email: String, password: String) {
        cookieJar.clear()

        val anonymous = api.get("/")
        if (anonymous.code >= 500) throw NetworkException("PepSchool indisponible (${anonymous.code})")
        if (cookieJar["PHPSESSID"] == null) throw AuthException("Impossible d'obtenir une session anonyme")

        val login = api.postForm(
            path = "/",
            fields = linkedMapOf(
                "email" to email,
                "password" to password,
                "checkbox" to "yes",
            ),
            referer = "${PepsApi.BASE_URL}/?logout=success",
            ajax = false,
        )
        if (login.code >= 500) throw NetworkException("PepSchool indisponible (${login.code})")
        if (!hasToken || HtmlUtils.isLoginPage(login.body)) {
            throw AuthException("Identifiants refusés par PepSchool")
        }
        store.put(SecureStore.KEY_LAST_AUTH, System.currentTimeMillis().toString())
        generation++
    }

    suspend fun <T> authorized(block: suspend () -> T): T {
        val seen = ensureSession()
        return try {
            block()
        } catch (e: SessionExpiredException) {
            reauthenticate(seen)
            block()
        }
    }

    suspend fun logout() {
        mutex.withLock {
            try {
                api.get("/?page=logout")
            } catch (_: Exception) {
            }
            cookieJar.clear()
            store.clearAll()
            generation++
        }
    }

    private companion object {
        const val REAUTH_INTERVAL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
