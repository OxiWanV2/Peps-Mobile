package be.lorenzogallo.pepsmobile.data.repository

import be.lorenzogallo.pepsmobile.data.model.AgendaOverview
import be.lorenzogallo.pepsmobile.data.model.Course
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.data.model.Lesson
import be.lorenzogallo.pepsmobile.data.model.NetworkException
import be.lorenzogallo.pepsmobile.data.model.NewsItem
import be.lorenzogallo.pepsmobile.data.model.PepsNotification
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.SchoolYear
import be.lorenzogallo.pepsmobile.data.model.SessionExpiredException
import be.lorenzogallo.pepsmobile.data.model.UserProfile
import be.lorenzogallo.pepsmobile.data.model.mondayOfWeek
import be.lorenzogallo.pepsmobile.data.network.PepsApi
import be.lorenzogallo.pepsmobile.data.network.SessionCookieJar
import be.lorenzogallo.pepsmobile.data.network.SessionManager
import be.lorenzogallo.pepsmobile.data.parser.AgendaParser
import be.lorenzogallo.pepsmobile.data.parser.CoursesParser
import be.lorenzogallo.pepsmobile.data.parser.HtmlUtils
import be.lorenzogallo.pepsmobile.data.parser.NewsParser
import be.lorenzogallo.pepsmobile.data.parser.NotificationsParser
import be.lorenzogallo.pepsmobile.data.parser.PanelParser
import be.lorenzogallo.pepsmobile.data.parser.ProfileParser
import be.lorenzogallo.pepsmobile.data.parser.parseAjaxObject
import be.lorenzogallo.pepsmobile.data.security.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class PepsRepository(
    private val api: PepsApi,
    private val session: SessionManager,
    private val store: SecureStore,
    private val cookieJar: SessionCookieJar,
) {
    private class Cached<T>(val fetchedAt: Long, val value: T) {
        fun fresh(ttl: Long) = System.currentTimeMillis() - fetchedAt < ttl
    }

    @Volatile private var coursesCache: List<Course>? = null
    @Volatile private var newsCache: Cached<List<NewsItem>>? = null
    @Volatile private var profileCache: UserProfile? = null
    @Volatile private var avatarCache: Cached<ByteArray?>? = null
    private val agendaCache = ConcurrentHashMap<LocalDate, Cached<List<Lesson>>>()
    private val pageCache = ConcurrentHashMap<String, Cached<PepsPage>>()

    fun hasStoredCredentials(): Boolean =
        !store.get(SecureStore.KEY_EMAIL).isNullOrBlank() && !store.get(SecureStore.KEY_PASSWORD).isNullOrEmpty()

    fun storedEmail(): String? = store.get(SecureStore.KEY_EMAIL)

    suspend fun login(email: String, password: String) = session.login(email.trim(), password)

    suspend fun logout() {
        clearCaches()
        session.logout()
    }

    fun clearCaches() {
        coursesCache = null
        newsCache = null
        profileCache = null
        avatarCache = null
        agendaCache.clear()
        pageCache.clear()
    }

    suspend fun fetchNews(force: Boolean = false): List<NewsItem> {
        if (!force) newsCache?.takeIf { it.fresh(CACHE_MS) }?.let { return it.value }
        val html = session.authorized { getPage("/index.php") }
        val (news, profile) = parse { NewsParser.parse(html) to ProfileParser.parse(html) }
        profile?.let { rememberProfile(it) }
        newsCache = Cached(System.currentTimeMillis(), news)
        return news
    }

    fun storedProfile(): UserProfile? {
        profileCache?.let { return it }
        val first = store.get(SecureStore.KEY_PROFILE_FIRST_NAME)
        val last = store.get(SecureStore.KEY_PROFILE_LAST_NAME)
        if (first.isNullOrBlank() && last.isNullOrBlank()) return null
        return UserProfile(first.orEmpty(), last.orEmpty()).also { profileCache = it }
    }

    suspend fun fetchProfile(): UserProfile? {
        profileCache?.let { return it }
        val html = session.authorized { getPage("/index.php") }
        val profile = parse { ProfileParser.parse(html) } ?: return storedProfile()
        rememberProfile(profile)
        return profile
    }

    suspend fun fetchAvatar(force: Boolean = false): ByteArray? {
        if (!force) avatarCache?.takeIf { it.fresh(AVATAR_CACHE_MS) }?.let { return it.value }
        val bytes = session.authorized {
            api.getBytes(ProfileParser.AVATAR_PATH, referer = "${PepsApi.BASE_URL}/?page=profil")
        }
        avatarCache = Cached(System.currentTimeMillis(), bytes)
        return bytes
    }

    private fun rememberProfile(profile: UserProfile) {
        profileCache = profile
        store.put(SecureStore.KEY_PROFILE_FIRST_NAME, profile.firstName)
        store.put(SecureStore.KEY_PROFILE_LAST_NAME, profile.lastName)
    }

    suspend fun fetchNotifications(): List<PepsNotification> = session.authorized {
        val body = ajax("getAll")
        parse { NotificationsParser.parse(body) }
    }

    suspend fun markNotificationRead(id: Long) = session.authorized { parseAjaxObject(ajax("markAsRead", id)); Unit }

    suspend fun markAllNotificationsRead() = session.authorized { parseAjaxObject(ajax("markAllAsRead")); Unit }

    suspend fun deleteNotification(id: Long) = session.authorized { parseAjaxObject(ajax("delete", id)); Unit }

    suspend fun deleteAllNotifications() = session.authorized { parseAjaxObject(ajax("deleteAll")); Unit }

    suspend fun fetchCourses(force: Boolean = false): List<Course> {
        if (!force) coursesCache?.let { return it }
        val html = session.authorized { getPage("/?page=cours") }
        val courses = parse { CoursesParser.parse(html) }
        coursesCache = courses
        return courses
    }

    suspend fun fetchAgenda(weekStart: LocalDate, force: Boolean = false): List<Lesson> {
        if (!force) agendaCache[weekStart]?.takeIf { it.fresh(CACHE_MS) }?.let { return it.value }
        val courseIds = fetchCourses().map { it.id }
        val body = session.authorized {
            val result = api.postMultipart(
                path = "/ajax.php",
                fields = linkedMapOf(
                    "action" to "agenda",
                    "actionAgenda" to "getHeureCoursStudent",
                    "dateStart" to weekStart.toString(),
                    "dateEnd" to weekStart.plusDays(6).toString(),
                    "listeCoursID" to courseIds.joinToString(",", "[", "]"),
                    "pageUrl" to "${PepsApi.BASE_URL}/?page=agenda",
                ),
                referer = "${PepsApi.BASE_URL}/?page=agenda",
            )
            if (result.code >= 500) throw NetworkException("PepSchool indisponible (${result.code})")
            parse { AgendaParser.parse(result.body) }
        }
        agendaCache[weekStart] = Cached(System.currentTimeMillis(), body)
        return body
    }

    suspend fun fetchAgendaOverview(weeksAhead: Int = 4, force: Boolean = false): AgendaOverview {
        val today = LocalDate.now()
        val year = SchoolYear.forDate(today)
        val currentMonday = year.clampWeek(today.mondayOfWeek())
        val weeks = (0..weeksAhead).map { currentMonday.plusWeeks(it.toLong()) }.filter { year.containsWeek(it) }
        val perWeek = coroutineScope { weeks.map { week -> async { fetchAgenda(week, force) } }.awaitAll() }
        val thisWeek = perWeek.firstOrNull().orEmpty()
        val upcoming = perWeek.drop(1).flatten()
            .filter { it.hasEvaluation || it.hasHomework }
            .sortedWith(compareBy({ it.date }, { it.start }))
        return AgendaOverview(thisWeek = thisWeek, upcoming = upcoming, weeksAhead = weeksAhead)
    }

    suspend fun fetchResults(path: String? = null, force: Boolean = false): PepsPage =
        fetchPanelPage(path ?: "/?page=resultats", force)

    suspend fun fetchCoursePage(courseId: Int, tab: CourseTab, periodPath: String? = null, force: Boolean = false): PepsPage =
        fetchPanelPage(periodPath ?: "/?page=cours&coursID=$courseId&pageCours=${tab.pageCours}", force)

    private suspend fun fetchPanelPage(path: String, force: Boolean): PepsPage {
        val safePath = requireSitePath(path)
        if (!force) pageCache[safePath]?.takeIf { it.fresh(CACHE_MS) }?.let { return it.value }
        val html = session.authorized { getPage(safePath) }
        val page = parse { PanelParser.parse(html) }
        pageCache[safePath] = Cached(System.currentTimeMillis(), page)
        return page
    }

    fun sessionCookies(): Map<String, String> = cookieJar.snapshot()

    private suspend fun <T> parse(block: () -> T): T = withContext(Dispatchers.Default) { block() }

    private fun requireSitePath(path: String): String {
        val normalized = PanelParser.normalizePath(path) ?: throw NetworkException("Chemin refusé")
        val url = (PepsApi.BASE_URL + normalized).toHttpUrlOrNull() ?: throw NetworkException("Chemin invalide")
        if (!url.isHttps || !url.host.equals(PepsApi.HOST, ignoreCase = true)) throw NetworkException("Hôte refusé")
        return normalized
    }

    private suspend fun getPage(path: String, referer: String = "${PepsApi.BASE_URL}/?login=success"): String {
        val result = api.get(path, referer)
        if (result.code >= 500) throw NetworkException("PepSchool indisponible (${result.code})")
        if (HtmlUtils.isLoginPage(result.body)) throw SessionExpiredException()
        return result.body
    }

    private suspend fun ajax(actionNotifications: String, id: Long? = null): String {
        val fields = linkedMapOf("action" to "notifications", "actionNotifications" to actionNotifications)
        if (id != null) fields["id"] = id.toString()
        val result = api.postForm("/ajax.php", fields, referer = "${PepsApi.BASE_URL}/?login=success", ajax = true)
        if (result.code >= 500) throw NetworkException("PepSchool indisponible (${result.code})")
        return result.body
    }

    private companion object {
        const val CACHE_MS = 5L * 60 * 1000
        const val AVATAR_CACHE_MS = 60L * 60 * 1000
    }
}
