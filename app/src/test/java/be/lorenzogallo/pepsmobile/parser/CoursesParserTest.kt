package be.lorenzogallo.pepsmobile.parser

import be.lorenzogallo.pepsmobile.data.parser.CoursesParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CoursesParserTest {
    private val html = """
        <html><body><section><div class="container ui-v2"><div class="row">
          <div class="col-6 cardCours"><div class="card">
            <a href="/?page=cours&coursID=101"><div class="card-header" style="background-color:#33d17a;color:#000000"><span>A.Martin</span></div></a>
            <div class="card-body"><h6 class="card-title"><a class="ps-link" href="/?page=cours&coursID=101"> Mathématique </a></h6></div>
          </div></div>
          <div class="col-6 cardCours"><div class="card">
            <a href="/?page=cours&coursID=102"><div class="card-header" style="background-color:#fff76b;color:#000000">
              <span data-bs-toggle="tooltip" data-bs-title="B.Durand&lt;br&gt;C.Petit">2 Enseignants</span></div></a>
            <div class="card-body"><h6 class="card-title"><a class="ps-link" href="/?page=cours&coursID=102">Histoire</a></h6></div>
          </div></div>
          <div class="col-6 cardCours"><div class="card">
            <a href="/?page=cours&coursID=103"><div class="card-header" style="background-color:#e66100;color:#ffffff"><span>A.Martin</span></div></a>
            <div class="card-body"><h6 class="card-title"><a class="ps-link" href="/?page=cours&coursID=103">Système d&#039;exploitation</a></h6></div>
          </div></div>
        </div></div></section></body></html>
    """.trimIndent()

    @Test
    fun `parses course cards with id name teachers and colors`() {
        val courses = CoursesParser.parse(html)
        assertEquals(listOf(101, 102, 103), courses.map { it.id })

        assertEquals("Mathématique", courses[0].name)
        assertEquals(listOf("A.Martin"), courses[0].teachers)
        assertEquals(0xFF33D17A.toInt(), courses[0].color)
        assertEquals(0xFF000000.toInt(), courses[0].textColor)

        assertEquals("Histoire", courses[1].name)
        assertEquals(listOf("B.Durand", "C.Petit"), courses[1].teachers)

        assertEquals("Système d'exploitation", courses[2].name)
        assertEquals(0xFFFFFFFF.toInt(), courses[2].textColor)
    }
}
