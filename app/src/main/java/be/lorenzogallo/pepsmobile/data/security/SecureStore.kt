package be.lorenzogallo.pepsmobile.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SecureStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val crypto = KeystoreCrypto()

    @Synchronized
    fun put(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }
        val encrypted = crypto.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(key, encrypted).apply()
    }

    @Synchronized
    fun get(key: String): String? {
        val encrypted = prefs.getString(key, null) ?: return null
        return try {
            String(crypto.decrypt(encrypted, key.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Valeur illisible pour '$key', suppression")
            prefs.edit().remove(key).apply()
            null
        }
    }

    @Synchronized
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().clear().commit()
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val PREFS_NAME = "peps_secure"

        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
        const val KEY_COOKIES = "cookies"
        const val KEY_LAST_AUTH = "last_auth"
        const val KEY_PROFILE_FIRST_NAME = "profile_first_name"
        const val KEY_PROFILE_LAST_NAME = "profile_last_name"
    }
}
