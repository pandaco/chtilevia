package fr.chtilevia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Écran principal : poser une question à la voix (ou au clavier), entendre et lire la réponse, lancer un favori */
class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val flow = QuestionFlow(this, speak = true) { model.conversation = it }
        setContent {
            ChtiTheme {
                Surface(Modifier.fillMaxSize()) { Screen(model, flow) }
            }
        }
        // adb shell am start -n fr.chtilevia/.android.MainActivity --ez mesurer true : mesure du GTFS complet
        if (savedInstanceState == null && intent.getBooleanExtra("mesurer", false)) model.measure()
    }

    override fun onStop() {
        super.onStop()
        model.stopSpeaking()
    }
}

@Composable
private fun Screen(model: MainViewModel, flow: QuestionFlow) {
    val conversation = model.conversation
    var typed by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var noRecognizer by remember { mutableStateOf(false) }

    Column(
        Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        model.status?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }

        Button(
            onClick = { noRecognizer = !flow.listen() },
            enabled = !conversation.busy,
            shape = CircleShape,
            modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally),
        ) { Text("🎤", fontSize = 56.sp) }
        if (noRecognizer) Text("Pas de reconnaissance vocale sur ce téléphone : écris ta question.")

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                label = { Text("Ou écris ta question") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = { flow.ask(typed.trim()) }, enabled = !conversation.busy && typed.isNotBlank()) { Text("OK") }
        }

        if (conversation.question.isNotEmpty()) Text("« ${conversation.question} »", style = MaterialTheme.typography.titleMedium)
        conversation.status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
            CircularProgressIndicator()
        }
        if (conversation.answer.isNotEmpty()) Text(conversation.answer, style = MaterialTheme.typography.bodyLarge)

        Text("Favoris", style = MaterialTheme.typography.titleMedium)
        if (model.favorites.isEmpty()) Text("Aucun favori pour l'instant.", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (name in model.favorites) {
                FilledTonalButton(onClick = { flow.favorite(name) }, enabled = !conversation.busy) { Text(name) }
            }
        }
        val text = editing
        if (text == null) {
            TextButton(onClick = { model.favoritesText { editing = it } }) { Text("Modifier les favoris") }
        } else {
            FavoritesEditor(
                text = text,
                onChange = { editing = it },
                onSave = {
                    model.saveFavorites(text)
                    editing = null
                },
                onCancel = { editing = null },
            )
        }
    }
}

@Composable
private fun FavoritesEditor(text: String, onChange: (String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Une ligne par favori, « nom: commande », ex. : travail: tram croise laroche vers roubaix",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onSave) { Text("Enregistrer") }
            TextButton(onClick = onCancel) { Text("Annuler") }
        }
    }
}
