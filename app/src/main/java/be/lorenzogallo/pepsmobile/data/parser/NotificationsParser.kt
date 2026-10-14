package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.PepsNotification
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

object NotificationsParser {
    fun parse(body: String): List<PepsNotification> {
        val root = parseAjaxObject(body)
        val list = root["message"] as? JsonArray ?: return emptyList()
        return list.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val id = o.long("id") ?: return@mapNotNull null
            PepsNotification(
                id = id,
                title = HtmlUtils.unescape(o.str("titre")),
                text = HtmlUtils.htmlToText(o.str("texte")),
                link = o.str("lien").trim().takeIf { it.isNotEmpty() },
                date = o.str("date"),
                read = o.bool("lue"),
            )
        }
    }
}
