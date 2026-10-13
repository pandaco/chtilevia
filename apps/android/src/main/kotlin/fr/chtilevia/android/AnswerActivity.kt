package fr.chtilevia.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Boutons du widget et raccourcis : écran invisible, le temps d'écouter (« Parler ») ou de demander
 * une autorisation, qui passe la commande à [AnswerService] et se ferme. La réponse arrive en
 * notification, et à voix haute pour « Parler » ; rien ne reste par-dessus l'écran d'accueil.
 */
class AnswerActivity : ComponentActivity(), Answerer {
    private lateinit var flow: QuestionFlow
    private var favorite: String? = null

    /** Faux au premier onResume : l'écoute ou la demande d'autorisation vient d'être lancée */
    private var started = false
    private var conversation = Conversation()

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { start() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        favorite = intent.getStringExtra(EXTRA_FAVORITE)
        flow = QuestionFlow(this, answerer = this) { conversation = it }
        if (savedInstanceState != null) return
        // La réponse arrive en notification : autorisation demandée une fois
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            start()
        }
    }

    private fun start() {
        val name = favorite
        when {
            name != null -> flow.favorite(name)
            !flow.listen() -> {
                Toast.makeText(this, "Pas de reconnaissance vocale sur ce téléphone.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    /** Écoute annulée (rien entendu) : on ferme sans rien dire */
    override fun onResume() {
        super.onResume()
        if (started && !conversation.busy && conversation.question.isEmpty()) finish()
        started = true
    }

    override fun command(question: String, args: List<String>, speak: Boolean) {
        AnswerService.answer(this, question, args, speak)
        finish()
    }

    override fun message(question: String, text: String, speak: Boolean) {
        AnswerService.say(this, question, text, speak)
        finish()
    }

    companion object {
        const val EXTRA_FAVORITE = "favori"
    }
}
