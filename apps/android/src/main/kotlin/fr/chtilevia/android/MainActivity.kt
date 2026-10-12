package fr.chtilevia.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.concurrent.thread

/** Écran provisoire de l'étape 8 : mesure du GTFS complet sur le téléphone */
class MainActivity : ComponentActivity() {
    private val lines = mutableStateListOf<String>()
    private val running = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AndroidChti(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Button(onClick = { measure(app) }, enabled = !running.value) { Text("Mesurer le GTFS complet") }
                        for (line in lines) Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        // adb shell am start -n fr.chtilevia/.android.MainActivity --ez mesurer true : mesure sans toucher l'écran
        if (savedInstanceState == null && intent.getBooleanExtra("mesurer", false)) measure(app)
    }

    private fun measure(app: AndroidChti) {
        running.value = true
        lines.clear()
        // Réseau et fichiers hors du fil principal
        thread(name = "mesure") {
            try {
                Benchmark(app, filesDir) { line -> runOnUiThread { lines += line } }.run()
            } catch (e: Exception) {
                runOnUiThread { lines += "Erreur : $e" }
            } finally {
                runOnUiThread { running.value = false }
            }
        }
    }
}
