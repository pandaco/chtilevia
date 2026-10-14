package fr.chtipasse.domain

/** Favori : un nom et les commandes chti qu'il lance, dans l'ordre */
data class Favorite(val name: String, val commands: List<String>)
