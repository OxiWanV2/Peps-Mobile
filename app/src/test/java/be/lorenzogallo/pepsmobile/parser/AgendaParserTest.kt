package be.lorenzogallo.pepsmobile.parser

import be.lorenzogallo.pepsmobile.data.model.SessionExpiredException
import be.lorenzogallo.pepsmobile.data.parser.AgendaParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AgendaParserTest {
    private val json = """
        {"error":false,"message":"N/A","output":0,"listeHeureCours":{
          "3":{"id":3,"cours_id":103,"intitule":"Système d&#039;exploitation","heureStart":"12:55","heureEnd":"13:45","date":"2026-10-12","local":"","color":"#61bd84","lecon":"","description":"","badges":{"evaluation":0,"devoir":0},"type":"cours"},
          "1":{"id":1,"cours_id":101,"intitule":"Informatique","heureStart":"08:25","heureEnd":"09:15","date":"2026-10-12","local":"","color":"#74aa8e","lecon":"","description":"Grève générale \r<br>","badges":{"evaluation":0,"devoir":0},"type":"cours"},
          "2":{"id":2,"cours_id":102,"intitule":"Anglais","heureStart":"09:15","heureEnd":"10:05","date":"2026-10-12","local":"","color":"#ff983d","lecon":"Listening","description":"","badges":{"evaluation":0,"devoir":1},"type":"cours"},
          "4":{"id":4,"cours_id":104,"intitule":"Sciences","heureStart":"13:45","heureEnd":"14:35","date":"2026-10-13","local":"","color":"#f4a4c0","lecon":"Evaluation","description":"Chapitres 1 &amp; 2","badges":{"evaluation":1,"devoir":0},"type":"cours"},
          "5":{"id":5,"cours_id":105,"intitule":"Programmation","heureStart":"10:20","heureEnd":"11:10","date":"2026-10-16","local":"A1|A2","color":"#e66100","lecon":"","description":"","badges":{"evaluation":0,"devoir":0},"type":"cours"}
        }}
    """.trimIndent()

    @Test
    fun `parses lessons sorted by date and time`() {
        val lessons = AgendaParser.parse(json)
        assertEquals(5, lessons.size)
        assertEquals(listOf("08:25", "09:15", "12:55", "13:45", "10:20"), lessons.map { it.start })
        assertEquals(LocalDate.of(2026, 10, 12), lessons[0].date)
        assertEquals("Grève générale", lessons[0].description)
        assertEquals(0xFF74AA8E.toInt(), lessons[0].color)
        assertTrue(lessons[1].hasHomework)
        assertEquals("Système d'exploitation", lessons[2].title)
        assertTrue(lessons[3].hasEvaluation)
        assertFalse(lessons[3].hasHomework)
        assertEquals("Chapitres 1 & 2", lessons[3].description)
        assertEquals("A1|A2", lessons[4].room)
    }

    @Test
    fun `empty week returns empty list`() {
        assertTrue(AgendaParser.parse("""{"error":false,"message":"N/A","output":0,"listeHeureCours":[]}""").isEmpty())
    }

    @Test(expected = SessionExpiredException::class)
    fun `html login page means session expired`() {
        AgendaParser.parse("""<html><form name="login"><input name="email"><input name="password"></form></html>""")
    }
}
