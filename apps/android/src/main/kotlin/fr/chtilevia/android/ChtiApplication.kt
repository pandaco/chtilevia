package fr.chtilevia.android

import android.app.Application
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Un seul cœur par processus : le référentiel en mémoire sert à l'écran comme au widget */
class ChtiApplication : Application() {
    val chti by lazy { AndroidChti(this) }

    /** Voix partagée par l'écran et le widget : une réponse coupe la précédente */
    val speaker by lazy { Speaker(this) }

    /** Fil des commandes : réseau et fichiers hors du fil principal, une commande à la fois */
    val worker: ExecutorService = Executors.newSingleThreadExecutor { Thread(it, "chti") }
}
