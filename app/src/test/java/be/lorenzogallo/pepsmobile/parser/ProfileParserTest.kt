package be.lorenzogallo.pepsmobile.parser

import be.lorenzogallo.pepsmobile.data.parser.ProfileParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileParserTest {
    @Test
    fun `reads first and last name from the side menu`() {
        val html = """
            <html><body><footer><div class="offcanvas"><div class="ps-userchip">
              <img class="ps-userchip__avatar" src="openTools.php?tools=avatar" alt="Avatar"/>
              <div class="ps-userchip__meta"><div class="ps-userchip__name">Dupont</div><div class="ps-userchip__sub">Alex</div></div>
            </div></div></footer></body></html>
        """.trimIndent()
        val profile = ProfileParser.parse(html)!!
        assertEquals("Alex", profile.firstName)
        assertEquals("Dupont", profile.lastName)
        assertEquals("Alex Dupont", profile.displayName)
    }

    @Test
    fun `returns null without user chip`() {
        assertNull(ProfileParser.parse("<html><body><p>login</p></body></html>"))
    }
}
