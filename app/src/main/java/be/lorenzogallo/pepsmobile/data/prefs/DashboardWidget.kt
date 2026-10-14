package be.lorenzogallo.pepsmobile.data.prefs

enum class DashboardWidget(val id: String, val label: String, val description: String, val system: Boolean) {
    TODAY("today", "Aujourd'hui", "Date, semaine, cours du jour et prochain cours", true),
    EVAL_WEEK("eval_week", "Évaluations de la semaine", "Évaluations du journal de classe de la semaine en cours", false),
    EVAL_UPCOMING("eval_upcoming", "Évaluations à venir", "Évaluations annoncées sur les 4 prochaines semaines", false),
    HOMEWORK("homework", "Devoirs", "Devoirs annoncés dans le journal de classe", false),
    RESULTS("results", "Résumé des résultats", "Moyenne et moyenne pondérée de la période", false),
    NEWS("news", "Actualités", "Communications de l'école", false),
    SHORTCUTS("shortcuts", "Raccourcis", "Accès rapide aux écrans et au site", true),
    ;

    companion object {
        fun byId(id: String): DashboardWidget? = entries.firstOrNull { it.id == id }
    }
}

data class DashboardWidgetConfig(val widget: DashboardWidget, val enabled: Boolean)
