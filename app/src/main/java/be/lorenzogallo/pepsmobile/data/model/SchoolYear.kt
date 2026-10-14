package be.lorenzogallo.pepsmobile.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

data class SchoolYear(val startYear: Int) {
    val firstWeekStart: LocalDate = mondayOfIsoWeek(startYear, START_WEEK)
    val lastWeekStart: LocalDate = mondayOfIsoWeek(startYear + 1, END_WEEK)
    val lastDay: LocalDate = lastWeekStart.plusDays(6)

    val label: String get() = "$startYear-${startYear + 1}"

    fun containsWeek(monday: LocalDate): Boolean =
        !monday.isBefore(firstWeekStart) && !monday.isAfter(lastWeekStart)

    fun clampWeek(monday: LocalDate): LocalDate = when {
        monday.isBefore(firstWeekStart) -> firstWeekStart
        monday.isAfter(lastWeekStart) -> lastWeekStart
        else -> monday
    }

    companion object {
        const val START_WEEK = 35
        const val END_WEEK = 26

        fun forDate(date: LocalDate): SchoolYear {
            val week = isoWeek(date)
            val weekYear = date.get(WeekFields.ISO.weekBasedYear())
            val startYear = when {
                week >= START_WEEK -> weekYear
                week <= END_WEEK -> weekYear - 1
                else -> weekYear
            }
            return SchoolYear(startYear)
        }

        fun isoWeek(date: LocalDate): Int = date.get(WeekFields.ISO.weekOfWeekBasedYear())

        fun mondayOfIsoWeek(weekBasedYear: Int, week: Int): LocalDate =
            LocalDate.of(weekBasedYear, 1, 4)
                .with(WeekFields.ISO.weekOfWeekBasedYear(), week.toLong())
                .with(ChronoField.DAY_OF_WEEK, 1)
    }
}

fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
