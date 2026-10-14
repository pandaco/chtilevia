package fr.chtipasse.domain

internal actual fun decomposeCompatibility(text: String): String = text.asDynamic().normalize("NFKD") as String
