package fr.chtilevia.android

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Raccourcis de l'appli (appui long sur l'icône) : « Parler » est fixe (res/xml/shortcuts.xml),
 * les favoris suivent le fichier des favoris, dans la limite que permet le lanceur.
 */
object Shortcuts {
    fun update(context: Context, favorites: List<String>) {
        val room = (ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) - 1).coerceAtLeast(0)
        val shortcuts = favorites.take(room).mapIndexed { rank, name ->
            val intent = Intent(context, AnswerActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(AnswerActivity.EXTRA_FAVORITE, name)
            ShortcutInfoCompat.Builder(context, "$FAVORITE$name")
                .setShortLabel(name)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .setRank(rank + 1)
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    /** Le lanceur met en avant les raccourcis les plus servis, d'où qu'on les lance */
    fun spoke(context: Context) = ShortcutManagerCompat.reportShortcutUsed(context, SPEAK)

    fun ranFavorite(context: Context, name: String) = ShortcutManagerCompat.reportShortcutUsed(context, "$FAVORITE$name")

    private const val SPEAK = "parler"
    private const val FAVORITE = "favori:"
}
