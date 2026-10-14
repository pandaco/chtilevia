package fr.chtilevia.android

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Synthèse vocale en français, quelle que soit la langue du téléphone. Lue comme une voix
 * d'assistant, avec la priorité audio le temps de la phrase : juste après la reconnaissance vocale,
 * la sortie audio peut encore être réduite ou occupée.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()
    private var ready = false
    private var failed = false
    private var pending: Pair<String, () -> Unit>? = null
    private val callbacks = ConcurrentHashMap<String, () -> Unit>()
    private var next = 0
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        failed = !ready
        Log.i(TAG, "synthèse vocale prête : $ready")
        if (ready) {
            val language = tts.setLanguage(Locale.FRANCE)
            if (language < TextToSpeech.LANG_AVAILABLE) Log.w(TAG, "voix française indisponible ($language)")
            tts.setAudioAttributes(attributes)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {
                    Log.i(TAG, "lecture commencée ($utteranceId)")
                }

                override fun onDone(utteranceId: String) = finished(utteranceId, "finie")

                @Deprecated("Remplacée par onError(String, Int), toujours appelée par les anciens moteurs")
                override fun onError(utteranceId: String) = finished(utteranceId, "en erreur")

                override fun onError(utteranceId: String, errorCode: Int) = finished(utteranceId, "en erreur ($errorCode)")

                override fun onStop(utteranceId: String, interrupted: Boolean) = finished(utteranceId, "arrêtée")
            })
        }
        pending?.let { (text, done) -> say(text, done) }
        pending = null
    }

    private fun finished(utteranceId: String, how: String) {
        Log.i(TAG, "lecture $how ($utteranceId)")
        audio.abandonAudioFocusRequest(focus)
        callbacks.remove(utteranceId)?.invoke()
    }

    /**
     * Lit [text] en coupant la lecture en cours ; [done] est appelé quand la lecture est finie,
     * interrompue ou impossible. Attend la fin de l'initialisation si besoin.
     */
    fun say(text: String, done: () -> Unit = {}) {
        when {
            failed -> done()
            !ready -> pending = text to done
            else -> {
                val id = "chti-${next++}"
                callbacks[id] = done
                val granted = audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                Log.i(TAG, "lecture demandée ($id) : ${if (queued == TextToSpeech.SUCCESS) "acceptée" else "refusée"}, priorité audio ${if (granted) "obtenue" else "refusée"}")
            }
        }
    }

    fun stop() {
        tts.stop()
        audio.abandonAudioFocusRequest(focus)
    }

    private companion object {
        const val TAG = "chtipasse"
    }
}
