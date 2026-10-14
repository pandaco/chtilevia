package fr.chtipasse.adapters

/** Nom des dossiers de l'appli (cache, configuration), et celui qu'ils portaient avant le renommage */
const val APP_DIRECTORY = "chtipasse"
const val LEGACY_APP_DIRECTORY = "chtilevia"

/**
 * Dossier [APP_DIRECTORY] de [base] (`~/.cache`, `~/.config`, fichiers de l'appli). L'ancien dossier
 * [LEGACY_APP_DIRECTORY], s'il existe seul, est renommé : favoris et horaires sont repris, sans
 * copie ni seconde source. Comme le shell (`app_dir`).
 */
fun appDirectory(base: String): String {
    val directory = "$base/$APP_DIRECTORY"
    val legacy = "$base/$LEGACY_APP_DIRECTORY"
    if (!LocalFiles.exists(directory) && LocalFiles.exists(legacy)) LocalFiles.rename(legacy, directory)
    return directory
}
