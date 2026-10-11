package fr.chtilevia.domain

/**
 * Forme de recherche d'un texte, identique au `fold` de chti : décomposition Unicode (NFKD),
 * caractères non ASCII retirés, minuscules, tout ce qui n'est ni lettre ni chiffre devient un
 * espace, espaces regroupés.
 *
 * « Croisé Laroche » et « croise-laroche » donnent tous deux « croise laroche ».
 * Comme chti, les lettres sans décomposition (œ, æ, ø, ß) disparaissent : « Cœur » → « cur ».
 */
fun fold(text: String): String =
    decomposeCompatibility(text)
        .filter { it.code < 128 }
        .lowercase()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ")

/** Décomposition de compatibilité Unicode (NFKD), fournie par chaque plateforme */
internal expect fun decomposeCompatibility(text: String): String
