package fr.chtipasse.domain

// Base des fuseaux horaires (Europe/Paris…) pour kotlinx-datetime sous Node : le module doit être
// chargé, ce que garantit l'export (voir le README de kotlinx-datetime, « time zones in JS »).
@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

@OptIn(ExperimentalJsExport::class)
@JsExport
val jsJodaTz = JsJodaTimeZoneModule
