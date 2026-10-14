package be.lorenzogallo.pepsmobile.ui.home

import android.net.Uri
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.data.network.PepsApi

object Routes {
    const val DASHBOARD = "dashboard"
    const val COURSES = "courses"
    const val AGENDA = "agenda"
    const val RESULTS = "results"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val DASHBOARD_SETTINGS = "settings/dashboard"
    const val WEB = "web/{url}"
    const val WEB_ARG = "url"
    const val COURSE = "course/{courseId}?tab={tab}&name={name}"
    const val COURSE_ID_ARG = "courseId"
    const val COURSE_TAB_ARG = "tab"
    const val COURSE_NAME_ARG = "name"

    val mainTabs = listOf(DASHBOARD, COURSES, AGENDA, RESULTS)

    fun course(courseId: Int, tab: CourseTab, name: String): String =
        "course/$courseId?tab=${tab.pageCours}&name=${Uri.encode(name)}"

    fun web(pathOrUrl: String): String? {
        val absolute = if (pathOrUrl.startsWith("http")) pathOrUrl else PepsApi.BASE_URL + "/" + pathOrUrl.removePrefix("/")
        val uri = Uri.parse(absolute)
        if (uri.scheme != "https" || !uri.host.equals(PepsApi.HOST, ignoreCase = true)) return null
        return "web/" + Uri.encode(absolute)
    }
}
