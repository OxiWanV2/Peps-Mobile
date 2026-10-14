package be.lorenzogallo.pepsmobile.agenda

import be.lorenzogallo.pepsmobile.data.model.SchoolYear
import be.lorenzogallo.pepsmobile.data.model.mondayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SchoolYearTest {
    @Test
    fun `school year runs from ISO week 35 to ISO week 26`() {
        val year = SchoolYear.forDate(LocalDate.of(2026, 10, 14))
        assertEquals(2026, year.startYear)
        assertEquals(LocalDate.of(2026, 8, 24), year.firstWeekStart)
        assertEquals(35, SchoolYear.isoWeek(year.firstWeekStart))
        assertEquals(26, SchoolYear.isoWeek(year.lastWeekStart))
        assertEquals(LocalDate.of(2027, 6, 28), year.lastWeekStart)
    }

    @Test
    fun `week numbering wraps 53 to 01 within the same school year`() {
        val year = SchoolYear(2026)
        var monday = year.firstWeekStart
        val weeks = mutableListOf<Int>()
        while (!monday.isAfter(year.lastWeekStart)) {
            weeks += SchoolYear.isoWeek(monday)
            monday = monday.plusWeeks(1)
        }
        assertEquals(35, weeks.first())
        assertEquals(26, weeks.last())
        assertTrue(weeks.contains(53))
        assertEquals(1, weeks[weeks.indexOf(53) + 1])
        assertEquals(weeks.size, weeks.distinct().size)
    }

    @Test
    fun `dates in spring belong to the previous start year`() {
        assertEquals(2025, SchoolYear.forDate(LocalDate.of(2026, 3, 10)).startYear)
        assertEquals(2026, SchoolYear.forDate(LocalDate.of(2026, 7, 20)).startYear)
    }

    @Test
    fun `clamp keeps navigation inside the school year`() {
        val year = SchoolYear(2026)
        assertEquals(year.firstWeekStart, year.clampWeek(LocalDate.of(2026, 8, 3).mondayOfWeek()))
        assertEquals(year.lastWeekStart, year.clampWeek(LocalDate.of(2027, 7, 20).mondayOfWeek()))
        assertTrue(year.containsWeek(LocalDate.of(2026, 10, 12)))
        assertFalse(year.containsWeek(LocalDate.of(2027, 7, 5)))
    }
}
