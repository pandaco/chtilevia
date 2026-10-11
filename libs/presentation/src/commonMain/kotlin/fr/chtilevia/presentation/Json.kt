package fr.chtilevia.presentation

/**
 * Document JSON minimal, imprimé exactement comme les sorties du contrat (`json.dumps(…, indent=2,
 * ensure_ascii=False)` côté shell) : deux espaces d'indentation, `[]` et `{}` pour les vides,
 * clés dans l'ordre d'insertion, caractères non ASCII tels quels.
 */
sealed interface Json {
    data class Obj(val fields: List<Pair<String, Json>>) : Json
    data class Arr(val items: List<Json>) : Json
    data class Str(val value: String) : Json
    data class Num(val value: Long) : Json
    data class Bool(val value: Boolean) : Json
    data object Null : Json

    companion object {
        fun obj(vararg fields: Pair<String, Json>) = Obj(fields.toList())
        fun arr(items: List<Json>) = Arr(items)
        fun str(value: String?) = value?.let(::Str) ?: Null
        fun num(value: Number?) = value?.let { Num(it.toLong()) } ?: Null
        fun bool(value: Boolean?) = value?.let(::Bool) ?: Null
    }
}

/** Texte du document, suivi d'un saut de ligne */
fun Json.render(): String = StringBuilder().also { write(it, this, 0) }.append('\n').toString()

private fun write(out: StringBuilder, value: Json, level: Int) {
    when (value) {
        is Json.Obj -> block(out, '{', '}', value.fields, level) { (key, item) ->
            string(out, key)
            out.append(": ")
            write(out, item, level + 1)
        }
        is Json.Arr -> block(out, '[', ']', value.items, level) { write(out, it, level + 1) }
        is Json.Str -> string(out, value.value)
        is Json.Num -> out.append(value.value)
        is Json.Bool -> out.append(value.value)
        Json.Null -> out.append("null")
    }
}

private fun <T> block(out: StringBuilder, open: Char, close: Char, items: List<T>, level: Int, item: (T) -> Unit) {
    out.append(open)
    if (items.isEmpty()) {
        out.append(close)
        return
    }
    items.forEachIndexed { index, it ->
        out.append(if (index == 0) "\n" else ",\n").append("  ".repeat(level + 1))
        item(it)
    }
    out.append('\n').append("  ".repeat(level)).append(close)
}

private fun string(out: StringBuilder, value: String) {
    out.append('"')
    for (c in value) {
        when (c) {
            '"' -> out.append("\\\"")
            '\\' -> out.append("\\\\")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            '\b' -> out.append("\\b")
            '\u000C' -> out.append("\\f")
            else -> if (c < ' ') out.append("\\u").append(c.code.toString(16).padStart(4, '0')) else out.append(c)
        }
    }
    out.append('"')
}
