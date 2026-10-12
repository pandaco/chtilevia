package fr.chtilevia.android

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Synthèse vocale en français, quelle que soit la langue du téléphone */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private var ready = false
    private var pending: String? = null
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return
        tts.language = Locale.FRANCE
        pending?.let(::say)
        pending = null
    }

    /** Lit [text] en coupant la lecture en cours ; attend la fin de l'initialisation si besoin */
    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "chti") else pending = text
    }

    fun stop() {
        tts.stop()
    }
}
