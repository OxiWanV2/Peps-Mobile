package be.lorenzogallo.pepsmobile.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import be.lorenzogallo.pepsmobile.MainActivity
import be.lorenzogallo.pepsmobile.R

data class WidgetContent(
    val title: String,
    val lines: List<String>,
    val footer: String?,
    val moreCount: Int = 0,
) {
    companion object {
        fun loading() = WidgetContent("Pep's Mobile", listOf("Chargement du journal de classe…"), null)
        fun loggedOut() = WidgetContent("Pep's Mobile", listOf("Ouvrez l'application pour vous connecter."), null)
        fun error(message: String) = WidgetContent("Pep's Mobile", listOf("Mise à jour impossible : $message"), "Appuyez pour réessayer")
    }
}

object WidgetRenderer {
    private val LINE_IDS = intArrayOf(R.id.widget_line1, R.id.widget_line2, R.id.widget_line3, R.id.widget_line4)

    fun render(context: Context, content: WidgetContent): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_agenda)
        views.setTextViewText(R.id.widget_title, content.title)
        LINE_IDS.forEachIndexed { index, id ->
            val line = content.lines.getOrNull(index)
            if (line == null) {
                views.setViewVisibility(id, View.GONE)
            } else {
                views.setViewVisibility(id, View.VISIBLE)
                views.setTextViewText(id, line)
            }
        }
        val footer = buildString {
            if (content.moreCount > 0) append("+${content.moreCount} autre(s) cours · ")
            content.footer?.let { append(it) }
        }
        views.setViewVisibility(R.id.widget_footer, if (footer.isBlank()) View.GONE else View.VISIBLE)
        views.setTextViewText(R.id.widget_footer, footer)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        val refresh = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, PepsWidgetProvider::class.java).setAction(PepsWidgetProvider.ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
        return views
    }
}
