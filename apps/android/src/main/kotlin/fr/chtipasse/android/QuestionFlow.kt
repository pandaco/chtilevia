package fr.chtipasse.android

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.Locale

/** Où en est une question : ce qui a été demandé, ce que l'on fait, la réponse */
data class Conversation(
    val question: String = "",
    val status: String? = null,
    val answer: String = "",
    /** Question « à proximité » : ce qu'il faut montrer sur la carte (« bus », « velo »… ; « » : tout) */
    val nearby: String? = null,
    /** La réponse en cartes (badges de passages…) ; vide : l'écran montre [answer] */
    val cards: List<AnswerCard> = emptyList(),
) {
    val busy get() = status != null
    val answered get() = answer.isNotEmpty() || cards.isNotEmpty()
}

/** Qui répond, quand ce n'est pas l'écran lui-même (widget : [AnswerService]) */
interface Answerer {
    /** La commande chti à lancer pour [question] (sans --voix ni --court) ; lue à voix haute si [speak] */
    fun command(question: String, args: List<String>, speak: Boolean)

    /** Un message tout fait (ex. position introuvable) */
    fun message(question: String, text: String, speak: Boolean)
}

/**
 * Une question, de l'écoute à la réponse, pour l'écran principal comme pour le widget : la
 * reconnaissance vocale (français), la phrase comprise comme `chti vocal`, la position du téléphone
 * quand elle parle d'« à proximité », puis la réponse affichée à pictos ; ou, avec [answerer], la
 * commande passée à qui répondra. On répond comme on a demandé : une question dite est aussi lue à
 * voix haute, un bouton touché ou une question tapée ne reçoit qu'une réponse écrite. À créer dans
 * `onCreate` (les demandes à Android y sont enregistrées).
 */
class QuestionFlow(
    private val activity: ComponentActivity,
    private val answerer: Answerer? = null,
    private val onUpdate: (Conversation) -> Unit,
) {
    private val app = activity.application as ChtiApplication
    private val main = Handler(Looper.getMainLooper())
    private var conversation = Conversation()
    /** Question « à proximité » en attente de l'autorisation de localisation, et sa commande */
    private var waitingForPosition: Pair<String, List<String>>? = null

    /** Vrai si la question en cours a été dite : la réponse sera aussi lue */
    private var speak = false

    private val listening = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val phrase = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (phrase == null) update(Conversation()) else ask(phrase, spoken = true)
    }

    private val locationPermission =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            val (phrase, words) = waitingForPosition ?: return@registerForActivityResult
            waitingForPosition = null
            if (granted.values.any { it }) answerNearby(phrase, words) else finish("Sans accès à ta position, je ne sais pas ce qui est à proximité.")
        }

    /** Ouvre la reconnaissance vocale ; false si le téléphone n'en a pas */
    fun listen(): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Ex. : prochain tram à Croisé Laroche vers Roubaix")
            // Laisser le temps de chercher ses mots : le réglage par défaut coupe à la moindre pause
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            .putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 4000L)
        return try {
            update(Conversation(status = "J'écoute…"))
            listening.launch(intent)
            Shortcuts.spoke(activity)
            true
        } catch (_: ActivityNotFoundException) {
            update(Conversation())
            false
        }
    }

    /** Phrase libre, dite ([spoken] : réponse lue, si la lecture n'est pas coupée) ou tapée (réponse écrite) */
    fun ask(phrase: String, spoken: Boolean = false) {
        speak = spoken && app.speakAnswers
        update(Conversation(question = phrase, status = "Je cherche…"))
        app.worker.execute {
            val words = app.chti.interpret(phrase)
            main.post {
                when {
                    // Phrase non comprise : « vocal » donne le message d'aide, à l'oral
                    words == null -> run(listOf("vocal", phrase))
                    words.firstOrNull() != "autour" -> run(words)
                    hasLocationPermission() -> answerNearby(phrase, words)
                    else -> {
                        waitingForPosition = phrase to words
                        locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            }
        }
    }

    /** Question déjà posée (notification touchée) : la commande relancée, réponse écrite */
    fun replay(question: String, args: List<String>) {
        speak = false
        val nearby = if (args.firstOrNull() == "autour") args.getOrNull(1)?.takeIf { it in NEARBY_WORDS }.orEmpty() else null
        update(Conversation(question = question, status = "Je cherche…", nearby = nearby))
        run(args)
    }

    /** Favori touché : réponse écrite */
    fun favorite(name: String) {
        speak = false
        update(Conversation(question = name, status = "Je cherche…"))
        Shortcuts.ranFavorite(activity, name)
        run(listOf(name))
    }

    private companion object {
        /** Modes de « autour » (voir InterpretUtterance) */
        val NEARBY_WORDS = setOf("bus", "tram", "metro", "velo")
    }

    private fun hasLocationPermission() = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    /** [words] : « autour » et, peut-être, le mode (« velo », « bus »…) ; la position du téléphone suit */
    private fun answerNearby(phrase: String, words: List<String>) {
        update(conversation.copy(question = phrase, status = "Je cherche ta position…", nearby = words.getOrElse(1) { "" }))
        app.worker.execute {
            val location = DeviceLocation.current(app)
            main.post {
                if (location == null) {
                    finish("Je ne trouve pas ta position : active la localisation et réessaie.")
                } else {
                    // Notation décimale fixe, point comme séparateur, quelle que soit la langue du téléphone
                    run(words + listOf("%.6f".format(Locale.ROOT, location.latitude), "%.6f".format(Locale.ROOT, location.longitude)))
                }
            }
        }
    }

    /**
     * Commande chti (sans --voix ni --court) : passée à [answerer], ou lancée ici. L'écran affiche la
     * réponse compacte à pictos (« 🚲 14 · 🅿️ 18 »), la voix lit les phrases (« 14 vélos disponibles »).
     */
    private fun run(args: List<String>) {
        answerer?.let {
            it.command(conversation.question, args, speak)
            return
        }
        val speak = speak
        app.worker.execute {
            val spoken = if (speak) clean(app.chti.run(listOf("--voix") + args).text) else null
            spoken?.let { main.post { app.speaker.say(it) } }
            // À l'écran, des cartes tirées du JSON (temps réel passage par passage), sinon le texte compact
            // (« vocal », pour une phrase non comprise, n'a pas de JSON : son message d'aide est un texte)
            val cards = if (args.first() == "vocal") null else AnswerCards.parse(app.chti.run(listOf("--json") + args).text)
            val written = if (cards.isNullOrEmpty()) clean(app.chti.run(listOf("--court") + args).text) else ""
            main.post { show(written, cards.orEmpty()) }
        }
    }

    /** Message tout fait (position introuvable…) : le même à l'écran et à l'oral */
    private fun finish(message: String) {
        answerer?.let {
            it.message(conversation.question, message, speak)
            return
        }
        show(message)
        if (speak) app.speaker.say(message)
    }

    private fun show(answer: String, cards: List<AnswerCard> = emptyList()) {
        update(conversation.copy(status = null, answer = answer, cards = cards))
    }

    /** Sans la ligne de mise à jour du référentiel, inutile à lire */
    private fun clean(text: String) =
        text.lines().filterNot { it.startsWith("Mise à jour du référentiel") }.joinToString("\n").trim()
            .ifEmpty { "Désolé, je n'ai pas de réponse." }

    private fun update(next: Conversation) {
        conversation = next
        onUpdate(next)
    }
}
