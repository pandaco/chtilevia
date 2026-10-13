package fr.chtilevia.application

import fr.chtilevia.domain.Favorite

/**
 * Fichier des favoris, partagé avec le shell : une ligne `nom: commande` par commande, `#` pour
 * les commentaires. Plusieurs lignes du même nom (casse ignorée) forment un favori à plusieurs
 * commandes.
 */
object FavoritesFile {
    private fun contentLines(text: String): List<String> =
        text.split('\n').let { if (text.endsWith('\n')) it.dropLast(1) else it }.map { it.substringBefore('#') }

    private val entry = Regex("""^\s*([^:]*?)\s*:\s*(.*?)\s*$""")

    /** Lignes utiles du fichier, sans commentaires ni espaces de fin, pour les montrer telles quelles */
    fun displayLines(text: String): List<String> =
        contentLines(text).map { it.trimEnd() }.filter { it.isNotEmpty() }

    /** Favoris, chacun sous le nom de sa première ligne, avec toutes ses commandes */
    fun favorites(text: String): List<Favorite> {
        val byName = LinkedHashMap<String, Favorite>()
        for (line in contentLines(text)) {
            val match = entry.find(line) ?: continue
            val (name, command) = match.destructured
            if (name.isEmpty() || command.isEmpty()) continue
            val key = name.asciiLowercase()
            byName[key] = byName[key]?.let { it.copy(commands = it.commands + command) } ?: Favorite(name, listOf(command))
        }
        return byName.values.toList()
    }

    /** Commandes du favori [name] (casse ignorée), vide s'il n'existe pas */
    fun commands(text: String, name: String): List<String> = contentLines(text).mapNotNull { line ->
        if (':' !in line) return@mapNotNull null
        val key = line.substringBefore(':').trim(' ', '\t').lowercase()
        if (key != name.lowercase()) return@mapNotNull null
        line.substringAfter(':').trimStart(' ', '\t').trimEnd(' ', '\t').ifEmpty { null }
    }

    /** Noms des favoris, tels qu'écrits dans le fichier */
    fun names(text: String): List<String> = contentLines(text)
        .filter { ':' in it }
        .map { it.substringBefore(':').trim() }
        .filter { it.isNotEmpty() }

    /** Mots d'une commande de favori, découpés comme le ferait le shell */
    fun words(command: String): List<String> = command.split(' ', '\t', '\n').filter { it.isNotEmpty() }

    /** Fichier qui contient [favorites], dans l'ordre : une ligne `nom: commande` par commande */
    fun format(favorites: List<Favorite>): String =
        favorites.flatMap { favorite -> favorite.commands.map { "${favorite.name}: $it\n" } }.joinToString("")

    /**
     * Fichier [text] réécrit pour contenir [favorites], dans leur ordre, sans perdre ce que la
     * personne y a écrit : l'en-tête (jusqu'à la dernière ligne vide avant le premier favori) et la
     * fin du fichier, les commentaires collés au-dessus d'une ligne de favori (qui la suivent si le
     * favori change de place), les lignes inchangées telles quelles (alignement et commentaire de
     * fin). Un favori renommé est retrouvé par ses questions. Seules les lignes nouvelles ou
     * renommées sont écrites à neuf ; les commentaires d'un favori supprimé partent avec lui.
     */
    fun update(text: String, favorites: List<Favorite>): String {
        val lines = text.split('\n').let { if (text.endsWith('\n')) it.dropLast(1) else it }
        val entries = lines.indices.filter { parse(lines[it]) != null }
        if (entries.isEmpty()) return text + (if (text.isEmpty() || text.endsWith('\n')) "" else "\n") + format(favorites)

        // Au-dessus de chaque ligne de favori : ses commentaires, précédés ou non d'une ligne vide
        val blocks = HashMap<Int, Block>()
        val firstBlank = (0 until entries.first()).lastOrNull { lines[it].isBlank() }
        val header = if (firstBlank == null) lines.subList(0, entries.first()) else lines.subList(0, firstBlank)
        blocks[entries.first()] = Block(firstBlank != null, if (firstBlank == null) emptyList() else lines.subList(firstBlank + 1, entries.first()))
        entries.zipWithNext { previous, index ->
            val between = lines.subList(previous + 1, index)
            blocks[index] = Block(between.any { it.isBlank() }, between.dropWhile { it.isBlank() }.filter { it.isNotBlank() })
        }

        val groups = entries.groupBy { parse(lines[it])!!.first.asciiLowercase() }
        val names = favorites.map { it.name.trim().asciiLowercase() }.toSet()
        val claimed = mutableSetOf<String>()
        val out = header.toMutableList()
        for (favorite in favorites) {
            val key = favorite.name.trim().asciiLowercase()
            // Renommé : l'ancien favori qui n'a plus de nom et partage le plus de questions
            val origin = key.takeIf { it in groups } ?: groups.keys
                .filter { it !in names && it !in claimed }
                .maxByOrNull { old -> groups.getValue(old).count { parse(lines[it])!!.second in favorite.commands } }
                ?.takeIf { old -> groups.getValue(old).any { parse(lines[it])!!.second in favorite.commands } }
            origin?.let { claimed += it }
            val own = origin?.let { groups.getValue(it) }.orEmpty().toMutableList()
            own.firstOrNull()?.let { out.addBlock(blocks.getValue(it)) }
            for (command in favorite.commands) {
                val index = own.firstOrNull { parse(lines[it])!!.second == command }
                if (index == null) {
                    out += "${favorite.name}: $command"
                    continue
                }
                val first = index == groups.getValue(origin!!).first()
                if (index != own.first()) out.addBlock(blocks.getValue(index))
                own -= index
                val line = lines[index]
                // La première ligne donne le nom affiché : réécrite si le nom a changé, même de casse
                out += if (key == origin && (!first || parse(line)!!.first == favorite.name.trim())) {
                    line
                } else {
                    "${favorite.name}: $command" + line.substringAfter('#', "").let { if (it.isEmpty()) "" else "   #$it" }
                }
            }
        }
        out += lines.subList(entries.last() + 1, lines.size)
        return out.joinToString("") { "$it\n" }
    }

    /** Commentaires au-dessus d'une ligne de favori, et la ligne vide qui les sépare de la précédente */
    private class Block(val blankBefore: Boolean, val comments: List<String>)

    private fun MutableList<String>.addBlock(block: Block) {
        if (block.blankBefore && isNotEmpty() && last().isNotBlank()) add("")
        addAll(block.comments)
    }

    /** Nom et commande d'une ligne de favori, ou null pour un commentaire, une ligne vide ou invalide */
    private fun parse(line: String): Pair<String, String>? {
        val match = entry.find(line.substringBefore('#')) ?: return null
        val (name, command) = match.destructured
        return if (name.isEmpty() || command.isEmpty()) null else name to command
    }

    /**
     * Commande à enregistrer pour les mots [words] d'une phrase comprise ([InterpretUtterance]) :
     * `-d` devient « vers », qui garde une direction en plusieurs mots une fois la ligne découpée
     */
    fun command(words: List<String>): String = words.joinToString(" ") { if (it == "-d") "vers" else it }

    /**
     * Commande de favori réécrite comme les propositions de saisie ([SuggestQuery]), pour la
     * modifier avec elles : `--prendre` → « pour prendre », `-d` → « vers »
     */
    fun phrase(command: String): String = words(command).joinToString(" ") {
        when (it) {
            "--prendre" -> "pour prendre"
            "--deposer", "--déposer" -> "pour déposer"
            "-d", "--direction", "--vers" -> "vers"
            else -> it
        }
    }

    /** Raison pour laquelle [name] ne peut pas nommer un favori parmi [others], ou null s'il convient */
    fun invalidName(name: String, others: List<String>): String? = when {
        name.isBlank() -> "Donne un nom au favori."
        ':' in name || '#' in name -> "Le nom ne peut contenir ni « : » ni « # »."
        others.any { it.asciiLowercase() == name.trim().asciiLowercase() } -> "Un autre favori porte déjà ce nom."
        else -> null
    }

    private fun String.asciiLowercase(): String = map { if (it in 'A'..'Z') it + 32 else it }.joinToString("")
}

/** Contenu de la liste des favoris : fichier présent et non vide, lignes à montrer, favoris */
data class FavoritesListing(
    val location: String,
    val isEmpty: Boolean,
    val lines: List<String>,
    val favorites: List<Favorite>,
)

class ListFavorites(private val store: FavoritesStore) {
    fun execute(): FavoritesListing {
        val text = store.read().orEmpty()
        return FavoritesListing(store.location, text.isEmpty(), FavoritesFile.displayLines(text), FavoritesFile.favorites(text))
    }
}

/** Commandes d'un favori ; vide s'il n'existe pas */
class FindFavorite(private val store: FavoritesStore) {
    fun execute(name: String): List<String> = FavoritesFile.commands(store.read().orEmpty(), name)
}
