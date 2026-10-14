package be.lorenzogallo.pepsmobile.parser

import be.lorenzogallo.pepsmobile.data.model.Tone
import be.lorenzogallo.pepsmobile.data.parser.GradeScale
import be.lorenzogallo.pepsmobile.data.parser.PanelParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelParserTest {
    private fun page(hub: String, periodHref: String, body: String) = """
        <html><body><section>
        <nav class="ui-v2 hub-bar"><div class="hub-left"><nav><ol class="breadcrumb">$hub</ol></nav></div>
          <div class="hub-right"><div class="hub-tabs">
            <a href="?page=cours&coursID=101&pageCours=evaluations" class="ps-tab is-active"><span>Évaluations</span></a>
            <a href="?page=cours&coursID=101&pageCours=statistiques" class="ps-tab"><span>Statistiques</span></a>
          </div></div></nav>
        <div class="container"><div class="row">
          <div class="ui-v2 col-12"><ul class="nav nav-tabs"><li class="nav-item"><a class="nav-link active" href="$periodHref">Période 1</a></li></ul></div>
          $body
        </div></div></section></body></html>
    """.trimIndent()

    private val courseHub = """<li class="breadcrumb-item"><div class="dropdown"><button class="ps-select"><span class="ps-select__text"><span class="ps-select__kicker">Cours</span><strong class="ps-select__value">Mathématique</strong></span></button></div></li>"""

    private fun score(value: String, max: String, pct: Int, tone: String) =
        """<div class="fw-semibold ps-res-note--$tone">$value<span class="ps-muted fw-normal">/$max</span></div><div class="ps-res-bar"><div class="ps-res-bar--$tone" style="width: $pct%"></div></div>"""

    private val evaluationsPage = page(
        courseHub,
        "/?page=cours&coursID=101&pageCours=evaluations&periodeID=5&appreciationID=0",
        """
        <div class="ui-v2 col-12"><section class="ps-panel">
          <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Synthèse</h2><div class="ps-panel__subtitle">Période : Période 1</div></div></header>
          <div class="ps-panel__body"><div class="ps-res-kpis">
            <div class="ps-kpi"><div class="ps-kpi__k">Moyenne du cours</div><div class="ps-kpi__v ps-res-note--danger">42/100</div></div>
            <div class="ps-kpi"><div class="ps-kpi__k">Évaluations</div><div class="ps-kpi__v">1</div></div>
          </div><p class="ps-muted"><i class="bi bi-info-circle"></i> La moyenne est donnée à titre informatif.</p></div>
        </section></div>
        <div class="ui-v2 col-12"><section class="ps-panel">
          <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Évaluations</h2></div></header>
          <div class="ps-panel__body"><div class="ps-eval-list"><div class="ps-eval ps-eval--danger">
            <div class="ps-eval__row">
              <div class="ps-eval__date ps-muted">22-09-2026</div>
              <div class="ps-eval__label"><span class="fw-semibold">Chapitre d&#039;introduction</span></div>
              <div class="ps-eval__note">${score("6.5", "20", 33, "danger")}</div>
            </div>
            <div class="collapse"><div class="ps-eval__detail"><table class="table">
              <tr><td><div class="fw-semibold">C1. Compétence un</div></td><td>${score("3.5", "4", 88, "success")}</td></tr>
              <tr><td><div class="fw-semibold">C2. Compétence deux</div></td><td>${score("3", "16", 19, "danger")}</td></tr>
            </table></div></div>
          </div></div></div>
        </section></div>
        """,
    )

    private val statisticsPage = page(
        courseHub,
        "/?page=cours&coursID=101&pageCours=statistiques&periodeID=5&appreciationID=0",
        """
        <div class="ui-v2 col-12"><section class="ps-panel">
          <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Compétences certificatives</h2></div></header>
          <div class="ps-panel__body"><table class="table">
            <thead><tr><th>Compétence</th><th>Moyenne</th></tr></thead>
            <tbody>
              <tr><td><div class="fw-semibold">Certificative</div><div class="ps-muted small">Certificative</div></td><td><span class="ps-muted">–</span></td></tr>
              <tr><td><div class="fw-semibold">C1. Compétence un</div><div class="ps-muted small">C1. Court</div></td><td><div class="fw-semibold ps-res-note--success">88%</div><div class="ps-res-bar"><div class="ps-res-bar--success" style="width: 88%"></div></div></td></tr>
              <tr class="ps-res-row--danger"><td><div class="fw-semibold">C2. Compétence deux</div></td><td><div class="fw-semibold ps-res-note--danger">19%</div></td></tr>
            </tbody>
          </table></div>
        </section></div>
        """,
    )

    private val resultsPage = """
        <html><body><section>
        <nav class="ui-v2 hub-bar"><div class="hub-right"><div class="hub-tabs">
          <a href="?page=resultats&pageResultats=resultats" class="ps-tab is-active"><span>Résultats</span></a>
          <a href="?page=resultats&pageResultats=bulletin" class="ps-tab"><span>Bulletin</span></a>
        </div></div></nav>
        <div class="container"><div class="row">
          <div class="ui-v2 col-12"><ul class="nav nav-tabs"><li><a class="nav-link active" href="/?page=resultats&periodeID=5&appreciationID=0">Période 1</a></li></ul></div>
          <div class="ui-v2 col-12"><section class="ps-panel">
            <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Synthèse</h2></div></header>
            <div class="ps-panel__body"><div class="ps-res-kpis">
              <div class="ps-kpi"><div class="ps-kpi__k">Moyenne</div><div class="ps-kpi__v">34/100</div></div>
              <div class="ps-kpi"><div class="ps-kpi__k">Cours évalués</div><div class="ps-kpi__v">5 / 12</div></div>
            </div></div>
          </section></div>
          <div class="ui-v2 col-12"><section class="ps-panel">
            <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Liste des cours</h2></div></header>
            <div class="ps-panel__body"><table class="table">
              <thead><tr><th colspan="2">Cours</th><th>Moyenne</th><th>Enseignants</th></tr></thead>
              <tbody>
                <tr class="ps-res-row--danger"><td><a class="ps-btn" href="?page=cours&amp;coursID=101"><i class="bi"></i></a></td><td class="fw-semibold">Mathématique</td>
                  <td>${score("42", "100", 42, "danger")}</td><td><div><span>A.Martin</span></div></td></tr>
                <tr><td><a class="ps-btn" href="?page=cours&amp;coursID=102"><i class="bi"></i></a></td><td class="fw-semibold">Histoire</td>
                  <td>${score("60", "100", 60, "success")}</td><td><div><span>B.Durand</span></div><div><span>C.Petit</span></div></td></tr>
                <tr><td><a class="ps-btn" href="?page=cours&amp;coursID=103"><i class="bi"></i></a></td><td class="fw-semibold">Géographie</td>
                  <td><span class="ps-muted">–</span></td><td><div><span>D.Leroy</span></div></td></tr>
              </tbody>
            </table></div>
          </section></div>
          <div class="ui-v2 col-12"><section class="ps-panel">
            <header class="ps-panel__header"><div class="ps-panel__titles"><h2 class="ps-panel__title">Graphique</h2></div></header>
            <div class="ps-panel__body"><div id="chartLoading"><div class="spinner-border"><span class="visually-hidden">Loading...</span></div></div>
              <script>window.addEventListener("load", function() {});</script>
              <p class="ps-muted">Ces données peuvent différer de la réalité.</p></div>
          </section></div>
        </div></div></section></body></html>
    """.trimIndent()

    @Test
    fun `course evaluations page`() {
        val page = PanelParser.parse(evaluationsPage)
        assertEquals("Mathématique", page.title)
        assertEquals(listOf("Évaluations", "Statistiques"), page.hubTabs.map { it.label })
        assertEquals("/?page=cours&coursID=101&pageCours=statistiques", page.hubTabs[1].path)
        assertTrue(page.hubTabs[0].active)
        assertEquals("/?page=cours&coursID=101&pageCours=evaluations&periodeID=5&appreciationID=0", page.periods.single().path)
        assertEquals(listOf("Synthèse", "Évaluations"), page.panels.map { it.title })

        val synthesis = page.panels[0]
        assertEquals("Période : Période 1", synthesis.subtitle)
        assertEquals("42/100", synthesis.kpis[0].value)
        assertEquals(Tone.DANGER, synthesis.kpis[0].tone)
        assertEquals(Tone.NEUTRAL, synthesis.kpis[1].tone)
        assertTrue(synthesis.notes.single().startsWith("La moyenne"))

        val eval = page.panels[1].evaluations.single()
        assertEquals("22-09-2026", eval.date)
        assertEquals("Chapitre d'introduction", eval.label)
        assertNotNull(eval.score)
        assertEquals("6.5/20", eval.score!!.display)
        assertEquals(33, eval.score!!.percent)
        assertEquals(Tone.DANGER, eval.score!!.tone)
        assertEquals(2, eval.rows.size)
        assertEquals("3.5/4", eval.rows[0].score!!.display)
        assertEquals(Tone.SUCCESS, eval.rows[0].tone)
        assertEquals(Tone.DANGER, eval.rows[1].tone)
        assertNull(eval.comment)
        assertTrue(page.panels[1].scoreRows.isEmpty())
    }

    @Test
    fun `course statistics page`() {
        val rows = PanelParser.parse(statisticsPage).panels.single().scoreRows
        assertEquals(3, rows.size)
        assertEquals("Certificative", rows[0].name)
        assertEquals("–", rows[0].score!!.display)
        assertNull(rows[0].score!!.percent)
        assertEquals("C1. Court", rows[1].detail)
        assertEquals(88, rows[1].score!!.percent)
        assertEquals(Tone.SUCCESS, rows[1].tone)
        assertEquals(Tone.DANGER, rows[2].tone)
        assertNull(rows[2].courseId)
    }

    @Test
    fun `results page`() {
        val page = PanelParser.parse(resultsPage)
        assertNull(page.title)
        assertEquals("/?page=resultats&pageResultats=bulletin", page.hubTabs[1].path)
        assertEquals(listOf("Synthèse", "Liste des cours"), page.panels.map { it.title })
        assertEquals(Tone.DANGER, page.panels[0].kpis[0].tone)
        assertEquals(Tone.NEUTRAL, page.panels[0].kpis[1].tone)

        val courses = page.panels[1].scoreRows
        assertEquals(3, courses.size)
        assertEquals("Mathématique", courses[0].name)
        assertEquals(101, courses[0].courseId)
        assertEquals("A.Martin", courses[0].detail)
        assertEquals(42, courses[0].score!!.percent)
        assertEquals(Tone.DANGER, courses[0].tone)
        assertEquals("B.Durand, C.Petit", courses[1].detail)
        assertEquals(Tone.WARNING, courses[1].tone)
        assertEquals(103, courses[2].courseId)
        assertEquals("–", courses[2].score!!.display)
        assertFalse(page.isEmpty)
    }

    @Test
    fun `grade scale and path normalization`() {
        assertEquals(Tone.DANGER, GradeScale.toneFor(49))
        assertEquals(Tone.WARNING, GradeScale.toneFor(50))
        assertEquals(Tone.WARNING, GradeScale.toneFor(64))
        assertEquals(Tone.SUCCESS, GradeScale.toneFor(65))
        assertEquals(33, PanelParser.percentOf("6.5/20"))
        assertEquals(88, PanelParser.percentOf("88%"))
        assertNull(PanelParser.percentOf("–"))
        assertEquals("/?page=resultats", PanelParser.normalizePath("?page=resultats"))
        assertNull(PanelParser.normalizePath("https://evil.example/?page=resultats"))
        assertNull(PanelParser.normalizePath("//evil.example/x"))
    }
}
