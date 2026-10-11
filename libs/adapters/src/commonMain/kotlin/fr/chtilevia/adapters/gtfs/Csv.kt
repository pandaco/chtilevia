package fr.chtilevia.adapters.gtfs

/**
 * Lecture CSV (RFC 4180, comme les fichiers GTFS) : guillemets doublés, champs sur plusieurs
 * lignes, BOM et fins de ligne \r\n tolérés. Chaque ligne devient une table colonne → valeur ;
 * une colonne absente de la ligne vaut "".
 */
internal fun csvRows(lines: Sequence<String>): Sequence<Map<String, String>> = sequence {
    var header: List<String>? = null
    val iterator = lines.iterator()
    while (iterator.hasNext()) {
        var line = iterator.next()
        if (header == null) line = line.removePrefix("﻿")
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (true) {
            if (i >= line.length) {
                // Fin de ligne dans un champ entre guillemets : le champ continue sur la ligne suivante
                if (quoted && iterator.hasNext()) {
                    field.append('\n')
                    line = iterator.next()
                    i = 0
                    continue
                }
                break
            }
            val c = line[i]
            when {
                quoted && c == '"' && line.getOrNull(i + 1) == '"' -> field.append('"').also { i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> fields += field.toString().also { field.clear() }
                !quoted && c == '\r' && i == line.length - 1 -> Unit
                else -> field.append(c)
            }
            i++
        }
        fields += field.toString()
        if (fields.size == 1 && fields[0].isEmpty()) continue // ligne vide
        val columns = header
        if (columns == null) {
            header = fields
        } else {
            yield(columns.indices.associate { columns[it] to fields.getOrElse(it) { "" } })
        }
    }
}
