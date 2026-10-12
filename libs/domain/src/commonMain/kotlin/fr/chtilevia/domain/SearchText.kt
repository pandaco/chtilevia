package fr.chtilevia.domain

/**
 * Forme de recherche d'un texte, identique au `fold` de chti : décomposition Unicode (NFKD),
 * caractères non ASCII retirés, minuscules, tout ce qui n'est ni lettre ni chiffre devient un
 * espace, espaces regroupés.
 *
 * « Croisé Laroche » et « croise-laroche » donnent tous deux « croise laroche ».
 * Les lettres sans décomposition (œ, æ, ø, ß) sont d'abord écrites en lettres simples : « Cœur » → « coeur ».
 */
fun fold(text: String): String =
    decomposeCompatibility(text.map { LIGATURES[it] ?: it.toString() }.joinToString(""))
        .filter { it.code < 128 }
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

/** Décomposition de compatibilité Unicode (NFKD), fournie par chaque plateforme */
internal expect fun decomposeCompatibility(text: String): String

/** Lettres sans décomposition Unicode et leur écriture en lettres simples, comme dans chti */
private val LIGATURES = mapOf(
    'œ' to "oe", 'Œ' to "OE", 'æ' to "ae", 'Æ' to "AE", 'ø' to "o", 'Ø' to "O", 'ß' to "ss", 'ẞ' to "SS",
)
