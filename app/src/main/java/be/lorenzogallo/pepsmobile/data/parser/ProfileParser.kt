package be.lorenzogallo.pepsmobile.data.parser

import be.lorenzogallo.pepsmobile.data.model.UserProfile
import org.jsoup.Jsoup

object ProfileParser {
    const val AVATAR_PATH = "/openTools.php?tools=avatar"

    fun parse(html: String): UserProfile? {
        val doc = Jsoup.parse(html)
        val chip = doc.selectFirst(".ps-userchip") ?: return null
        val lastName = chip.selectFirst(".ps-userchip__name")?.text()?.trim().orEmpty()
        val firstName = chip.selectFirst(".ps-userchip__sub")?.text()?.trim().orEmpty()
        if (lastName.isEmpty() && firstName.isEmpty()) return null
        return UserProfile(firstName = firstName, lastName = lastName)
    }
}
