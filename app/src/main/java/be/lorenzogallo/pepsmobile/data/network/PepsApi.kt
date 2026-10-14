package be.lorenzogallo.pepsmobile.data.network

import be.lorenzogallo.pepsmobile.data.model.NetworkException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class PepsApi(cookieJar: CookieJar) {
    data class HttpResult(val code: Int, val body: String)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .followRedirects(true)
        .followSslRedirects(false)
        .addNetworkInterceptor(HostGuardInterceptor)
        .build()

    suspend fun get(path: String, referer: String? = null): HttpResult {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .headers(navigationHeaders(referer))
            .get()
            .build()
        return execute(request)
    }

    suspend fun getBytes(path: String, referer: String? = null, maxBytes: Long = MAX_BINARY_BYTES): ByteArray? {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .headers(
                Headers.Builder().apply {
                    add("User-Agent", USER_AGENT)
                    add("Accept", "image/avif,image/webp,image/png,image/*;q=0.8,*/*;q=0.5")
                    add("Accept-Language", ACCEPT_LANGUAGE)
                    add("Sec-Fetch-Dest", "image")
                    add("Sec-Fetch-Mode", "no-cors")
                    add("Sec-Fetch-Site", "same-origin")
                    if (referer != null) add("Referer", referer)
                }.build(),
            )
            .get()
            .build()
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body ?: return@use null
                    if (response.code != 200) return@use null
                    val type = body.contentType()?.type
                    if (type != "image") return@use null
                    if (body.contentLength() > maxBytes) return@use null
                    val source = body.source()
                    if (!source.request(maxBytes + 1)) source.readByteArray() else null
                }
            } catch (e: IOException) {
                throw NetworkException("Connexion à PepSchool impossible", e)
            }
        }
    }

    suspend fun postForm(
        path: String,
        fields: Map<String, String>,
        referer: String?,
        ajax: Boolean,
    ): HttpResult {
        val body = FormBody.Builder(Charsets.UTF_8).apply {
            fields.forEach { (k, v) -> add(k, v) }
        }.build()
        val request = Request.Builder()
            .url(BASE_URL + path)
            .headers(if (ajax) ajaxHeaders(referer) else navigationHeaders(referer))
            .post(body)
            .build()
        return execute(request)
    }

    suspend fun postMultipart(
        path: String,
        fields: Map<String, String>,
        referer: String?,
    ): HttpResult {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            fields.forEach { (k, v) -> addFormDataPart(k, v) }
        }.build()
        val request = Request.Builder()
            .url(BASE_URL + path)
            .headers(ajaxHeaders(referer))
            .post(body)
            .build()
        return execute(request)
    }

    private suspend fun execute(request: Request): HttpResult = withContext(Dispatchers.IO) {
        try {
            client.newCall(request).execute().use { response: Response ->
                HttpResult(code = response.code, body = response.body?.string().orEmpty())
            }
        } catch (e: IOException) {
            throw NetworkException("Connexion à PepSchool impossible", e)
        }
    }

    private fun navigationHeaders(referer: String?): Headers = Headers.Builder().apply {
        add("User-Agent", USER_AGENT)
        add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        add("Accept-Language", ACCEPT_LANGUAGE)
        add("Upgrade-Insecure-Requests", "1")
        add("Sec-Fetch-Dest", "document")
        add("Sec-Fetch-Mode", "navigate")
        add("Sec-Fetch-Site", if (referer == null) "none" else "same-origin")
        add("Sec-Fetch-User", "?1")
        add("Sec-GPC", "1")
        if (referer != null) add("Referer", referer)
    }.build()

    private fun ajaxHeaders(referer: String?): Headers = Headers.Builder().apply {
        add("User-Agent", USER_AGENT)
        add("Accept", "*/*")
        add("Accept-Language", ACCEPT_LANGUAGE)
        add("Origin", BASE_URL)
        add("X-Requested-With", "XMLHttpRequest")
        add("Sec-Fetch-Dest", "empty")
        add("Sec-Fetch-Mode", "cors")
        add("Sec-Fetch-Site", "same-origin")
        add("Referer", referer ?: "$BASE_URL/?login=success")
    }.build()

    private object HostGuardInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val url = chain.request().url
            if (!url.isHttps || !url.host.equals(HOST, ignoreCase = true)) {
                throw IOException("Hôte non autorisé : ${url.host}")
            }
            return chain.proceed(chain.request())
        }
    }

    companion object {
        const val HOST = "sjbsm.pepschool.be"
        const val BASE_URL = "https://$HOST"
        const val USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:152.0) Gecko/20100101 Firefox/152.0"
        private const val ACCEPT_LANGUAGE = "fr-BE,fr;q=0.9,en-US;q=0.7,en;q=0.5"
        private const val MAX_BINARY_BYTES = 2L * 1024 * 1024
    }
}
