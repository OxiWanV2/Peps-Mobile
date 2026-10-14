package be.lorenzogallo.pepsmobile.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class NavigationRequests {
    val agendaDate = MutableStateFlow<LocalDate?>(null)

    fun clear() {
        agendaDate.value = null
    }
}
