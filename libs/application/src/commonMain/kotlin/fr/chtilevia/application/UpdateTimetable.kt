package fr.chtilevia.application

/** Retélécharge le référentiel théorique ; renvoie son emplacement */
class UpdateTimetable(private val updater: TimetableUpdater) {
    fun execute(notices: Notices = Notices.NONE): String = updater.refresh(force = true, notices = notices)
}
