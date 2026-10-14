package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.NewsItem
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object NewsParser {
    private val TYPE = Regex("ps-news--([A-Za-z0-9_-]+)")
    private val WELCOME_TITLE = Regex("^\\s*bienvenue\\b", RegexOption.IGNORE_CASE)

    fun parse(html: String): List<NewsItem> {
        val doc = Jsoup.parse(html)
        var blocks = doc.select("section:has(.ps-panel__title)").toList()
        if (blocks.isEmpty()) blocks = doc.select("[class*=ps-panel]:has(.ps-panel__title)").toList()

        val innermost = blocks.filter { block -> blocks.none { other -> other !== block && other.parents().contains(block) } }

        return innermost.filterNot { isWelcomeTile(it) }.mapNotNull { block -> parseBlock(block) }
    }

    private fun isWelcomeTile(block: Element): Boolean {
        if (block.selectFirst("[data-bs-target=#collapseNewsbienvenue]") != null) return true
        val title = block.selectFirst(".ps-panel__title")?.text().orEmpty()
        return WELCOME_TITLE.containsMatchIn(title)
    }

    private fun parseBlock(block: Element): NewsItem? {
        val title = block.selectFirst(".ps-panel__title")?.text()?.trim()
            ?.takeIf { it.isNotEmpty() } ?: return null

        val classNames = buildString {
            append(block.className())
            block.parents().take(2).forEach { append(' ').append(it.className()) }
            block.select("[class*=ps-news--]").forEach { append(' ').append(it.className()) }
        }
        val type = TYPE.find(classNames)?.groupValues?.get(1) ?: "info"

        val badge = block.selectFirst(".badge")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val subtitle = block.selectFirst(".ps-panel__subtitle")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val body = block.selectFirst(".ps-panel__body")?.let { HtmlUtils.textWithLineBreaks(it) }.orEmpty()
        val date = block.selectFirst("i.bi-calendar3")?.parent()?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val recipients = block.select("span[style*=.72rem]")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() && !it.contains("Destinataire", ignoreCase = true) }

        return NewsItem(
            type = type,
            badge = badge,
            title = title,
            subtitle = subtitle,
            body = body,
            date = date,
            recipients = recipients,
        )
    }
}
