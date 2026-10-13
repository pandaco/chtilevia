package fr.chtilevia.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Boutons du widget : une carte en bas de l'écran, par-dessus l'accueil, le temps de la question.
 * « Parler » écoute et lit la réponse ; un favori (extra « favori ») l'affiche sans voix. La carte
 * reste à l'écran jusqu'à ce qu'on la touche : l'appli reste au premier plan pendant la réponse.
 */
class AnswerActivity : ComponentActivity() {
    private var conversation by mutableStateOf(Conversation())
    private lateinit var flow: QuestionFlow

    /** Faux au premier onResume : l'écoute ou la question vient d'être lancée */
    private var started = false

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val favorite = intent.getStringExtra(EXTRA_FAVORITE)
        flow = QuestionFlow(this, speak = favorite == null) { conversation = it }
        setContent {
            ChtiTheme {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).clickable { finishIfDone() },
                    contentAlignment = Alignment.BottomCenter,
                ) { AnswerCard() }
            }
        }
        if (savedInstanceState != null) return
        // Les notifications (réponse gardée) sont demandées une fois ; la réponse s'affiche de toute façon
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start()
        }
    }

    private fun start() {
        val favorite = intent.getStringExtra(EXTRA_FAVORITE)
        when {
            favorite != null -> flow.favorite(favorite)
            !flow.listen() -> conversation = Conversation(answer = "Pas de reconnaissance vocale sur ce téléphone.")
        }
    }

    /** Écoute annulée (rien entendu) : rien à montrer, on ferme */
    override fun onResume() {
        super.onResume()
        val c = conversation
        if (started && !c.busy && c.question.isEmpty() && c.answer.isEmpty()) finish()
        started = true
    }

    private fun finishIfDone() {
        if (!conversation.busy) {
            (application as ChtiApplication).speaker.stop()
            finish()
        }
    }

    @Composable
    private fun AnswerCard() {
        val c = conversation
        Card(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp).clickable { finishIfDone() }) {
            Column(
                Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (c.question.isNotEmpty()) Text("« ${c.question} »", style = MaterialTheme.typography.titleMedium)
                c.status?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (c.answer.isNotEmpty()) Text(c.answer, style = MaterialTheme.typography.bodyLarge)
                if (!c.busy) TextButton(onClick = { finishIfDone() }, modifier = Modifier.align(Alignment.End)) { Text("Fermer") }
            }
        }
    }

    companion object {
        const val EXTRA_FAVORITE = "favori"
    }
}
