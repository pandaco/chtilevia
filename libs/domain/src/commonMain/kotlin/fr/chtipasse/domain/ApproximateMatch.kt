package fr.chtipasse.domain

/*
 * Rapprochement approximatif d'une demande et d'un nom (fautes de frappe, reconnaissance vocale),
 * identique à celui de chti. Il ne sert que quand aucun nom ne contient la demande ([fold]).
 */

/** Phonétique française simple, règles appliquées dans l'ordre à un mot sous forme [fold] */
private val SOUND_RULES = listOf(
    "eaux" to "o", "eau" to "o", "au" to "o", "ph" to "f", "ch" to "sh", "qu" to "k", "ck" to "k",
    "c(?=[eiy])" to "s", "c" to "k", "gu(?=[eiy])" to "g", "ai" to "e", "ei" to "e", "y" to "i",
    "(?<![cs])h" to "", "(.)\\1" to "$1", "(?<=.)er$" to "", "(?<=.)[esxtdz]+$" to "",
).map { (pattern, replacement) -> Regex(pattern) to replacement }

/** Forme sonore d'un mot : « maur » et « mort » donnent tous deux « mor » */
fun sound(word: String): String = SOUND_RULES.fold(word) { acc, (pattern, replacement) -> pattern.replace(acc, replacement) }

/** Distance d'édition (Levenshtein) */
fun editDistance(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val current = IntArray(b.length + 1)
        current[0] = i
        for (j in 1..b.length) {
            val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
        }
        previous = current
    }
    return previous[b.length]
}

/** Écart toléré pour un mot : aucun jusqu'à 3 lettres, 1 jusqu'à 7, 2 au-delà */
private fun tolerance(word: String): Int = when {
    word.length <= 3 -> 0
    word.length <= 7 -> 1
    else -> 2
}

/**
 * Écart entre deux suites de mots, ou null si trop éloignées. Meilleur de :
 * - la demande collée comparée à des mots consécutifs du nom collés (« croise la roche » ≈ « laroche ») ;
 * - mot à mot : chaque mot de plus de 2 lettres de la demande ressemble à un mot du nom.
 */
private fun wordGap(query: List<String>, name: List<String>): Int? {
    val gaps = mutableListOf<Int>()
    val joined = query.joinToString("")
    val best = name.indices.minOf { i ->
        (i + 1..name.size).minOf { j -> editDistance(joined, name.subList(i, j).joinToString("")) }
    }
    if (best <= tolerance(joined)) gaps += best
    val words = query.filter { it.length > 2 }
    if (words.isNotEmpty()) {
        val perWord = words.map { word -> name.minOf { editDistance(word, it) } }
        if (perWord.zip(words).all { (gap, word) -> gap <= tolerance(word) }) gaps += perWord.sum()
    }
    return gaps.minOrNull()
}

/** Nombres dits en lettres (forme [fold]) et leur écriture en chiffres */
val NUMBER_WORDS = mapOf(
    "un" to "1", "une" to "1", "deux" to "2", "trois" to "3", "quatre" to "4", "cinq" to "5",
    "six" to "6", "sept" to "7", "huit" to "8", "neuf" to "9", "dix" to "10", "onze" to "11",
    "douze" to "12", "treize" to "13", "quatorze" to "14", "quinze" to "15", "seize" to "16",
    "dix sept" to "17", "dix huit" to "18", "dix neuf" to "19", "vingt" to "20",
)

/** Nombres en lettres écrits en chiffres, comme dans les noms d'arrêts : « quatre cantons » → « 4 cantons » */
private fun inDigits(words: List<String>): List<String> {
    val out = mutableListOf<String>()
    var i = 0
    while (i < words.size) {
        val pair = NUMBER_WORDS[words.subList(i, minOf(i + 2, words.size)).joinToString(" ")]
        if (i + 1 < words.size && pair != null) {
            out += pair
            i += 2
        } else {
            out += NUMBER_WORDS[words[i]] ?: words[i]
            i += 1
        }
    }
    return out
}

/**
 * Écart entre une demande et un nom, à l'écrit ([fold]) ou au son ([sound]), nombres en lettres ou
 * en chiffres ; null si sans rapport
 */
fun approximateGap(query: String, name: String): Int? {
    val q = fold(query).split(' ').filter { it.isNotEmpty() }
    val n = fold(name).split(' ').filter { it.isNotEmpty() }
    if (q.isEmpty() || n.isEmpty()) return null
    return listOf(q, inDigits(q)).flatMap { v -> listOfNotNull(wordGap(v, n), wordGap(v.map(::sound), n.map(::sound))) }.minOrNull()
}

/** Éléments au nom le plus proche de la demande (tous ceux à égalité), vide si aucun ne ressemble */
fun <T> Iterable<T>.closestTo(query: String, names: (T) -> Iterable<String>): List<T> {
    val gaps = mapNotNull { item -> names(item).mapNotNull { approximateGap(query, it) }.minOrNull()?.let { item to it } }
    val best = gaps.minOfOrNull { it.second } ?: return emptyList()
    return gaps.filter { it.second == best }.map { it.first }
}
