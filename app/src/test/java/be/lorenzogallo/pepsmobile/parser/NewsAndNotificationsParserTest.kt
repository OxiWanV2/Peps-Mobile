package be.lorenzogallo.pepsmobile.parser

import be.lorenzogallo.pepsmobile.data.parser.NewsParser
import be.lorenzogallo.pepsmobile.data.parser.NotificationsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsAndNotificationsParserTest {
    private val dashboard = """
        <html><body><section><div class="container ui-v2">
          <div class="col-12 ui-v2"><section class="ps-panel">
            <header class="ps-panel__header" data-bs-toggle="collapse" data-bs-target="#collapseNewsbienvenue">
              <div class="ps-panel__titles"><h2 class="ps-panel__title">Bienvenue Alex</h2><p class="ps-panel__subtitle">Clique sur le titre des tuiles</p></div>
            </header>
            <div class="collapse" id="collapseNewsbienvenue"><div class="ps-panel__body">Mode d'emploi.</div></div>
          </section></div>
          <div class="col-12 ui-v2"><section class="ps-panel ps-news ps-news--warning">
            <span class="badge">Important</span>
            <h2 class="ps-panel__title">Grève du 12 octobre</h2>
            <p class="ps-panel__subtitle">Direction</p>
            <div class="ps-panel__body">Les cours sont suspendus.<br>Merci de votre compréhension &amp; bonne journée.</div>
            <p><i class="bi bi-calendar3"></i> 10/10/2026</p>
            <span style="font-size:.72rem">Destinataires :</span>
            <span style="font-size:.72rem">Classe A</span>
            <span style="font-size:.72rem">Parents</span>
          </section></div>
          <div class="col-12 ui-v2"><section class="ps-panel">
            <h2 class="ps-panel__title">Sortie scolaire</h2>
            <div class="ps-panel__body">Rendez-vous à 8h.</div>
          </section></div>
        </div></section></body></html>
    """.trimIndent()

    @Test
    fun `parses news blocks and skips the welcome tile`() {
        val news = NewsParser.parse(dashboard)
        assertEquals(2, news.size)
        assertTrue(news.none { it.title.startsWith("Bienvenue") })

        val first = news[0]
        assertEquals("warning", first.type)
        assertEquals("Important", first.badge)
        assertEquals("Grève du 12 octobre", first.title)
        assertEquals("Direction", first.subtitle)
        assertEquals("Les cours sont suspendus.\nMerci de votre compréhension & bonne journée.", first.body)
        assertEquals("10/10/2026", first.date)
        assertEquals(listOf("Classe A", "Parents"), first.recipients)

        assertEquals("info", news[1].type)
        assertNull(news[1].badge)
        assertEquals("Sortie scolaire", news[1].title)
    }

    @Test
    fun `parses notifications`() {
        val body = """{"error":false,"message":[
            {"id":12,"titre":"Nouvelle note","texte":"Math &amp; sciences","lien":"/?page=resultats","date":"2026-10-13 09:15:00","lue":0},
            {"id":13,"titre":"Info","texte":"","lien":"","date":"2026-10-12 08:00:00","lue":true}
        ]}"""
        val notifications = NotificationsParser.parse(body)
        assertEquals(2, notifications.size)
        assertEquals("Math & sciences", notifications[0].text)
        assertEquals("/?page=resultats", notifications[0].link)
        assertFalse(notifications[0].read)
        assertNull(notifications[1].link)
        assertTrue(notifications[1].read)
    }
}
