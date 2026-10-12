package fr.chtilevia.android

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Widget d'accueil, comme Termux:Widget : « 🎤 Parler », puis un bouton par favori */
class ChtiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val favorites = (context.applicationContext as ChtiApplication).chti.favoriteNames()
        provideContent { GlanceTheme { Content(favorites) } }
    }

    @Composable
    private fun Content(favorites: List<String>) {
        LazyColumn(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.widgetBackground).cornerRadius(16.dp).padding(8.dp),
        ) {
            item { Button("🎤 Parler", actionStartActivity<AnswerActivity>(), GlanceModifier.fillMaxWidth()) }
            if (favorites.isEmpty()) {
                item {
                    Text(
                        "Ajoute tes favoris dans l'appli",
                        GlanceModifier.fillMaxWidth().padding(top = 8.dp).clickable(actionStartActivity<MainActivity>()),
                        TextStyle(color = GlanceTheme.colors.onSurface),
                    )
                }
            }
            for (name in favorites) {
                item {
                    Button(
                        name,
                        actionStartActivity<AnswerActivity>(actionParametersOf(FAVORITE to name)),
                        GlanceModifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        }
    }

    companion object {
        /** Passé à [AnswerActivity] : le favori à lancer */
        private val FAVORITE = ActionParameters.Key<String>(AnswerActivity.EXTRA_FAVORITE)

        /** À appeler quand les favoris changent */
        suspend fun refresh(context: Context) = ChtiWidget().updateAll(context)
    }
}

class ChtiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChtiWidget()
}
