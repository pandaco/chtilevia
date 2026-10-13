package fr.chtilevia.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Une famille de questions : à quoi elle sert, et des phrases à dire telles quelles */
private class Topic(val title: String, val text: String, val examples: List<String>)

private val TOPICS = listOf(
    Topic(
        "Prochains passages",
        "Le mode (bus, tram, métro, liane), la ligne, l'arrêt, et si tu veux la direction après « vers ».",
        listOf("prochain tram à Croisé Laroche vers Roubaix", "bus 10 Gambetta", "métro 1 République vers CHU", "liane 1 République"),
    ),
    Topic(
        "V'Lille",
        "Vélos et places d'une station. Ajoute « pour prendre » ou « pour déposer » pour n'avoir que l'un ou l'autre.",
        listOf("vélo Rihour", "V'Lille Gare Lille Europe pour prendre", "vélo Sainte Hélène pour déposer"),
    ),
    Topic(
        "À proximité",
        "Les arrêts et stations les plus proches de toi, avec ta position. Précise vélo, bus, tram ou métro pour ne voir que ceux-là.",
        listOf("à proximité", "vélo à proximité", "bus autour de moi", "le métro le plus proche"),
    ),
    Topic(
        "Favoris",
        "Dis simplement le nom d'un favori. Tu les crées et les modifies sur l'écran principal (« Modifier »).",
        listOf("travail"),
    ),
)

/**
 * Page d'aide : ce que l'on peut demander, avec des exemples à toucher pour les essayer ([onTry]),
 * et comment l'appli répond.
 */
@Composable
fun HelpScreen(onTry: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Que demander ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Fermer") }
        }
        Text(
            "Parle naturellement : les fautes et les mots en trop sont pardonnés. Touche un exemple pour l'essayer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        for (topic in TOPICS) {
            Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 20.dp))
            Text(topic.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (example in topic.examples) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onTry(example) },
                    ) { Text("« $example »", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) }
                }
            }
        }
        Text("Comment chti répond", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 28.dp))
        Text(
            "• Question dite au micro : réponse lue à voix haute, et écrite.\n" +
                "• Bouton touché ou question écrite : réponse écrite seulement (🚲 vélos, 🅿️ places libres, 📍 arrêts proches).\n" +
                "• Widget : « Parler » écoute ta question, chaque favori a son bouton ; la réponse arrive en notification.\n" +
                "• Appui long sur l'icône de l'appli : « Parler » et tes favoris.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
