package fr.chtilevia.adapters.favorites

import fr.chtilevia.application.FavoritesStore
import java.io.File

/** Favoris dans un fichier texte, au format du shell (`~/.config/chtilevia/favoris` par défaut) */
class FileFavoritesStore(private val file: File) : FavoritesStore {
    override val location: String = file.path

    override fun read(): String? = file.takeIf { it.isFile }?.readText()
}
