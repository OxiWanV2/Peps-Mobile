package be.lorenzogallo.pepsmobile.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser

object HtmlUtils {
    private const val BR_SENTINEL = "\uE000BR\uE000"
    private val MULTI_NEWLINES = Regex("\n{3,}")
    private val BR_TAG = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)

    fun isLoginPage(html: String): Boolean =
        html.contains("name=\"password\"") && html.contains("name=\"email\"") && html.contains("name=\"login\"")

    fun textWithLineBreaks(element: Element): String {
        val copy = element.clone()
        copy.select("br").forEach { it.replaceWith(TextNode(BR_SENTINEL)) }
        copy.select("p, div, li, tr, h1, h2, h3, h4, h5, h6").forEach { it.appendChild(TextNode(BR_SENTINEL)) }
        val text = copy.text()
        return text.split(BR_SENTINEL)
            .joinToString("\n") { it.trim() }
            .replace(MULTI_NEWLINES, "\n\n")
            .trim()
    }

    fun htmlToText(fragment: String): String {
        if (fragment.isBlank()) return ""
        val doc = Jsoup.parseBodyFragment(fragment.replace("\r", ""))
        return textWithLineBreaks(doc.body())
    }

    fun unescape(text: String): String = Parser.unescapeEntities(text, false).trim()

    fun splitOnBr(text: String): List<String> =
        text.split(BR_TAG).map { unescape(it) }.filter { it.isNotEmpty() }

    fun styleProperty(style: String, property: String): String? {
        val regex = Regex("(?:^|;)\\s*${Regex.escape(property)}\\s*:\\s*([^;]+)", RegexOption.IGNORE_CASE)
        return regex.find(style)?.groupValues?.get(1)?.trim()
    }

    fun parseHexColor(value: String?, fallback: Int): Int {
        val hex = value?.trim()?.removePrefix("#") ?: return fallback
        val expanded = when (hex.length) {
            3 -> hex.map { "$it$it" }.joinToString("")
            6 -> hex
            else -> return fallback
        }
        val rgb = expanded.toIntOrNull(16) ?: return fallback
        return (0xFF shl 24) or rgb
    }

    const val DEFAULT_COURSE_COLOR: Int = 0xFF607D8B.toInt()
    const val BLACK: Int = 0xFF000000.toInt()
}
