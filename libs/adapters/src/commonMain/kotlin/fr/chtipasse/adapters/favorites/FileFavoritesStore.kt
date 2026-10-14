package fr.chtipasse.adapters.favorites

import fr.chtipasse.adapters.LocalFiles
import fr.chtipasse.application.FavoritesStore

/** Favoris dans un fichier texte, au format du shell (`~/.config/chtipasse/favoris` par défaut) */
class FileFavoritesStore(private val path: String) : FavoritesStore {
    override val location: String = path

    override fun read(): String? = LocalFiles.readText(path)
}
