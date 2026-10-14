package fr.chtipasse.android

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

/** Widget d'accueil, comme Termux:Widget : « Parler » en rouge, puis un bouton par favori, sur fond blanc (noir la nuit) */
class ChtiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val favorites = (context.applicationContext as ChtiApplication).chti.favoriteNames()
        provideContent { Content(favorites) }
    }

    @Composable
    private fun Content(favorites: List<String>) {
        Column(GlanceModifier.fillMaxSize().background(BACKGROUND).cornerRadius(20.dp).padding(10.dp)) {
            // « Parler » : le geste principal, toujours visible
            Row(
                GlanceModifier.fillMaxWidth().height(52.dp).background(RED).cornerRadius(26.dp).clickable(actionStartActivity<AnswerActivity>()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(ImageProvider(R.drawable.ic_mic), contentDescription = null, GlanceModifier.size(24.dp), colorFilter = ColorFilter.tint(ON_RED))
                Spacer(GlanceModifier.width(8.dp))
                Text("Parler", style = TextStyle(color = ON_RED, fontSize = 17.sp, fontWeight = FontWeight.Bold))
            }
            if (favorites.isEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    "Ajoute tes favoris dans l'appli",
                    GlanceModifier.fillMaxWidth().padding(4.dp).clickable(actionStartActivity<MainActivity>()),
                    TextStyle(color = ON_BACKGROUND, fontSize = 13.sp),
                )
            }
            LazyColumn(GlanceModifier.defaultWeight().fillMaxWidth()) {
                for (name in favorites) {
                    item {
                        // Espace fixe au-dessus de chaque bouton : les boutons ne se touchent jamais
                        Column(GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                            FavoriteButton(name, actionStartActivity<AnswerActivity>(actionParametersOf(FAVORITE to name)))
                        }
                    }
                }
            }
            Text(
                "Que demander ?",
                GlanceModifier.fillMaxWidth().padding(top = 6.dp).clickable(actionStartActivity<MainActivity>(actionParametersOf(HELP to true))),
                TextStyle(color = ON_BACKGROUND, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
        }
    }

    @Composable
    private fun FavoriteButton(name: String, onClick: Action) {
        Box(
            GlanceModifier.fillMaxWidth().height(44.dp).background(TONAL).cornerRadius(14.dp).clickable(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(name, style = TextStyle(color = ON_TONAL, fontSize = 15.sp, fontWeight = FontWeight.Medium), maxLines = 1)
        }
    }

    companion object {
        /** Passé à [AnswerActivity] : le favori à lancer */
        private val FAVORITE = ActionParameters.Key<String>(AnswerActivity.EXTRA_FAVORITE)

        /** Passé à [MainActivity] : ouvrir la page d'aide */
        private val HELP = ActionParameters.Key<Boolean>(MainActivity.EXTRA_HELP)

        // Couleurs de l'icône (voir Theme.kt), en clair et en sombre
        private val BACKGROUND = ColorProvider(day = Color.White, night = Brand.night)
        private val ON_BACKGROUND = ColorProvider(day = Brand.muted, night = Brand.nightMuted)
        private val RED = ColorProvider(day = Brand.red, night = Brand.red)
        private val ON_RED = ColorProvider(day = Color.White, night = Color.White)
        private val TONAL = ColorProvider(day = Brand.field, night = Brand.nightField)
        private val ON_TONAL = ColorProvider(day = Brand.red, night = Color.White)

        /** À appeler quand les favoris changent */
        suspend fun refresh(context: Context) = ChtiWidget().updateAll(context)
    }
}

class ChtiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChtiWidget()
}
