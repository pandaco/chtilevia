package fr.chtipasse.android

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.chtipasse.application.NearbyMode
import fr.chtipasse.application.QuerySuggestions

/** Écran principal : poser une question à la voix (ou au clavier), entendre et lire la réponse, lancer un favori */
class MainActivity : ComponentActivity() {
    private val model: MainViewModel by viewModels()
    private lateinit var flow: QuestionFlow

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        flow = QuestionFlow(this) { model.conversation = it }
        setContent {
            val dark = model.darkTheme ?: isSystemInDarkTheme()
            // Barres du système (heure, batterie) lisibles sur le fond du thème choisi, pas celui du téléphone
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            ChtiTheme(dark) {
                Surface(Modifier.fillMaxSize()) { Screen(model, flow) }
            }
        }
        // adb shell am start -n fr.chtipasse/.android.MainActivity --ez mesurer true : mesure du GTFS complet
        if (savedInstanceState == null && intent.getBooleanExtra("mesurer", false)) model.measure()
        if (savedInstanceState == null) openFrom(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openFrom(intent)
    }

    /** Lien « Que demander ? » du widget : la page d'aide ; notification touchée : la même question */
    private fun openFrom(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_HELP, false)) model.page = MainViewModel.Page.HELP
        val args = intent.getStringArrayListExtra(EXTRA_ARGS)
        if (args != null) {
            model.page = MainViewModel.Page.MAIN
            flow.replay(intent.getStringExtra(EXTRA_QUESTION).orEmpty(), args)
        }
    }

    companion object {
        const val EXTRA_HELP = "aide"
        private const val EXTRA_QUESTION = "question"
        private const val EXTRA_ARGS = "commande"

        /** Ouvre l'appli sur [question] ([args] : sa commande chti, relancée) ; juste l'appli sans [args] */
        fun reopen(context: Context, question: String, args: List<String>?): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (args != null) intent.putExtra(EXTRA_QUESTION, question).putStringArrayListExtra(EXTRA_ARGS, ArrayList(args))
            return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onStop() {
        super.onStop()
        model.stopSpeaking()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Screen(model: MainViewModel, flow: QuestionFlow) {
    val context = LocalContext.current
    var pendingMap by remember { mutableStateOf<NearbyMode?>(null) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val mode = pendingMap ?: return@rememberLauncherForActivityResult
        pendingMap = null
        if (granted.values.any { it }) model.openMap(mode)
    }
    val showMap = { mode: NearbyMode ->
        val allowed = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (allowed) {
            model.openMap(mode)
        } else {
            pendingMap = mode
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    // Un seul gestionnaire de « précédent », qui défait la dernière chose ouverte : d'abord le clavier,
    // puis la question de favori en cours, le favori en cours, la carte, l'aide ou les favoris, la barre
    // de saisie, la réponse ; sur l'accueil, l'appli se ferme
    val ime = WindowInsets.isImeVisible
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val conversation = model.conversation
    val answering = conversation.question.isNotEmpty() || conversation.answered
    BackHandler(
        enabled = ime || model.page != MainViewModel.Page.MAIN || model.typing ||
            (answering && !conversation.busy),
    ) {
        when {
            ime -> {
                keyboard?.hide()
                focus.clearFocus()
            }
            model.page == MainViewModel.Page.FAVORITES && model.editedQuestion != null -> model.closeQuestion()
            model.page == MainViewModel.Page.FAVORITES && model.draft != null -> model.draft = null
            model.page != MainViewModel.Page.MAIN -> model.page = MainViewModel.Page.MAIN
            model.typing -> model.typing = false
            else -> {
                model.stopSpeaking()
                model.conversation = Conversation()
            }
        }
    }
    when (model.page) {
        MainViewModel.Page.HELP -> HelpScreen(
            onTry = {
                // L'exemple s'affiche en résultats, en plein écran : pas de barre de saisie ni de clavier
                model.typing = false
                model.page = MainViewModel.Page.MAIN
                flow.ask(it)
            },
            onBack = { model.page = MainViewModel.Page.MAIN },
        )
        MainViewModel.Page.FAVORITES -> FavoritesScreen(model)
        MainViewModel.Page.MAP -> MapScreen(model.map, onMode = showMap, onSelect = model::selectPlace, onBack = { model.page = MainViewModel.Page.MAIN })
        MainViewModel.Page.MAIN -> Home(model, flow, onMap = showMap, onHelp = { model.page = MainViewModel.Page.HELP }, onPlace = model::openPlace)
    }
}

@Composable
private fun Home(
    model: MainViewModel,
    flow: QuestionFlow,
    onMap: (NearbyMode) -> Unit,
    onHelp: () -> Unit,
    onPlace: (Place, String) -> Unit,
) {
    val conversation = model.conversation
    // Texte de la barre de saisie, avec la position du curseur (pré-rempli quand on touche la question)
    var typed by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val field = remember { FocusRequester() }
    var noRecognizer by remember { mutableStateOf(false) }
    val listen = { noRecognizer = !flow.listen() }
    val send = {
        if (typed.text.isNotBlank() && !conversation.busy) {
            flow.ask(typed.text.trim())
            typed = TextFieldValue("")
            model.typing = false
        }
    }
    // Clavier ouvert sur le champ seulement à la demande (« Écrire », question touchée), une fois
    LaunchedEffect(model.typing, model.focusField) {
        if (model.typing && model.focusField) {
            runCatching { field.requestFocus() }
            model.focusField = false
        }
    }
    // Question touchée : dans la barre de saisie, curseur à la fin, pour la corriger ou la compléter
    val rephrase: (String) -> Unit = { question ->
        typed = TextFieldValue(question, selection = TextRange(question.length))
        model.suggest(question)
        model.typing = true
        model.focusField = true
    }
    // Au repos, le micro occupe le centre ; dès qu'il y a une question, il descend dans la barre du bas
    val idle = conversation.question.isEmpty() && !conversation.answered && conversation.status == null
    // « ← Retour » : comme le geste « précédent » (voir Screen), on ferme la réponse
    val back = {
        model.stopSpeaking()
        model.conversation = Conversation()
    }
    val dark = model.darkTheme ?: isSystemInDarkTheme()

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo()
            Spacer(Modifier.size(10.dp))
            Text("Chtipasse", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = model::toggleSpeech) {
                Icon(
                    painterResource(if (model.speakAnswers) R.drawable.ic_volume_on else R.drawable.ic_volume_off),
                    contentDescription = if (model.speakAnswers) "Réponses lues : couper" else "Réponses non lues : activer",
                    tint = if (model.speakAnswers) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { model.toggleTheme(dark) }) {
                Icon(
                    painterResource(if (dark) R.drawable.ic_light_mode else R.drawable.ic_dark_mode),
                    contentDescription = if (dark) "Passer en thème clair" else "Passer en thème sombre",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onMap(NearbyMode.ALL) }) { IconLabel(R.drawable.ic_map, "Carte") }
            TextButton(onClick = onHelp) { Text("Aide") }
        }
        model.status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp))
        }

        if (model.typing) {
            // Saisie : le champ tout en haut, l'étape et les propositions en direct en dessous
            TypingPanel(
                typed = typed,
                onChange = {
                    typed = it
                    model.suggest(it.text)
                },
                suggestions = model.suggestions,
                field = field,
                enabled = !conversation.busy,
                onSend = send,
                onClose = { model.typing = false },
                placeholder = "bus 10 gambetta, vélo à proximité…",
            )
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    idle -> {
                        Spacer(Modifier.height(56.dp))
                        MicButton(size = 168.dp, enabled = true, onClick = listen)
                        Spacer(Modifier.height(28.dp))
                        Text(
                            if (noRecognizer) "Pas de reconnaissance vocale sur ce téléphone : écris ta question." else "Touche le micro et pose ta question",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "« prochain tram à Croisé Laroche vers Roubaix »\n« vélo à proximité »",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    else -> {
                        if (!conversation.busy) {
                            TextButton(onClick = back, modifier = Modifier.align(Alignment.Start)) { Text("←  Retour") }
                        }
                        Spacer(Modifier.height(4.dp))
                        Answer(conversation, onPlace, onRephrase = rephrase)
                        val nearby = conversation.nearby
                        if (nearby != null && conversation.answered) {
                            TextButton(onClick = { onMap(NEARBY_MODES[nearby] ?: NearbyMode.ALL) }) { IconLabel(R.drawable.ic_map, "Voir sur la carte") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            FavoritesBar(
                names = model.favorites,
                enabled = !conversation.busy,
                onRun = flow::favorite,
                onEdit = { model.openFavorites(create = model.favorites.isEmpty()) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                TextButton(onClick = {
                    model.suggest(typed.text)
                    model.typing = true
                    model.focusField = true
                }, modifier = Modifier.align(Alignment.CenterStart)) { Text("⌨  Écrire") }
                if (!idle) MicButton(size = 64.dp, enabled = !conversation.busy, onClick = listen, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

/**
 * Saisie d'une question : le champ en haut, puis l'étape de la requête (« Quelle ligne ? », « Quel
 * arrêt ? »…) et les propositions à toucher pour la compléter. Sert aussi aux questions des favoris
 * ([sendIcon] : valider plutôt qu'envoyer ; [error] : pourquoi la question n'est pas acceptée).
 */
@Composable
fun ColumnScope.TypingPanel(
    typed: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    suggestions: QuerySuggestions,
    field: FocusRequester,
    enabled: Boolean,
    onSend: () -> Unit,
    onClose: () -> Unit,
    placeholder: String,
    sendIcon: Int = R.drawable.ic_send,
    sendLabel: String = "Demander",
    error: String? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = typed,
            onValueChange = onChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.weight(1f).focusRequester(field),
        )
        FilledIconButton(onClick = onSend, enabled = enabled && typed.text.isNotBlank(), modifier = Modifier.size(48.dp)) {
            Icon(painterResource(sendIcon), contentDescription = sendLabel)
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            error ?: suggestions.step,
            style = if (error != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClose) { Text("Fermer") }
    }
    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
        items(suggestions.items, key = { it.text }) { suggestion ->
            Text(
                suggestion.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
                    .clickable { onChange(TextFieldValue(suggestion.text, selection = TextRange(suggestion.text.length))) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )
            HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outline)
        }
    }
}

/** Bouton texte avec une icône sobre devant, de la couleur du texte */
@Composable
fun IconLabel(icon: Int, label: String) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.size(8.dp))
    Text(label)
}

@Composable
private fun MicButton(size: Dp, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier.size(size).shadow(if (size > 100.dp) 12.dp else 4.dp, CircleShape),
    ) { Icon(painterResource(R.drawable.ic_mic), contentDescription = "Parler", modifier = Modifier.size(size * 0.43f)) }
}

/** La question, ce qui se passe, puis la réponse : une carte par ligne de la réponse compacte */
@Composable
private fun Answer(conversation: Conversation, onPlace: (Place, String) -> Unit, onRephrase: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (conversation.question.isNotEmpty()) {
            // La question se touche : elle passe dans la barre de saisie pour être corrigée ou complétée
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !conversation.busy) { onRephrase(conversation.question) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!conversation.busy) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = "Modifier la question",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    "« ${conversation.question} »",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        conversation.status?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
        if (conversation.cards.isNotEmpty()) {
            for (card in conversation.cards) AnswerCardView(card, onPlace)
            if (conversation.cards.any { it.badges.isNotEmpty() }) {
                Row(
                    Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(painterResource(R.drawable.ic_live), contentDescription = null, tint = Brand.live, modifier = Modifier.size(16.dp))
                    Text("temps réel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.size(8.dp))
                    Icon(
                        painterResource(R.drawable.ic_schedule),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("horaire théorique", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            for (line in conversation.answer.lines().filter { it.isNotBlank() }) AnswerLine(line)
        }
    }
}

/**
 * Une carte de réponse : le sujet en petit, puis la réponse en grand ou les passages en badges ;
 * la toucher ouvre la carte sur l'arrêt ou la station
 */
@Composable
private fun AnswerCardView(card: AnswerCard, onPlace: (Place, String) -> Unit) {
    val place = card.place
    val detail = card.value ?: card.subtitle.orEmpty()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().then(if (place != null) Modifier.clickable { onPlace(place, detail) } else Modifier),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (place != null) {
                    Icon(
                        painterResource(R.drawable.ic_map),
                        contentDescription = "Voir sur la carte",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            card.subtitle?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            card.value?.let { Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (card.badges.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) { for (badge in card.badges) DepartureBadge(badge) }
            }
        }
    }
}

/** Passage en badge : vert plein et ondes si temps réel, gris sans fond et horloge si horaire théorique */
@Composable
private fun DepartureBadge(badge: Badge) {
    val live = badge.realtime
    Surface(
        color = if (live) Brand.live else MaterialTheme.colorScheme.surface,
        contentColor = if (live) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(50),
        border = if (live) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painterResource(if (live) R.drawable.ic_live else R.drawable.ic_schedule),
                contentDescription = if (live) "temps réel" else "horaire théorique",
                modifier = Modifier.size(18.dp),
            )
            Text(badge.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Une ligne de la réponse compacte (« sujet : réponse ») : le sujet en petit, la réponse en grand.
 * Les phrases (messages d'erreur, d'aide) restent telles quelles.
 */
@Composable
private fun AnswerLine(line: String) {
    val cut = line.lastIndexOf(" : ").takeIf { it > 0 && !line.trimEnd().endsWith(".") }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (cut == null) {
                Text(line, style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(line.substring(0, cut), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(line.substring(cut + 3), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Les favoris en pastilles qui défilent, au-dessus de la barre du bas, et de quoi les modifier */
@Composable
private fun FavoritesBar(names: List<String>, enabled: Boolean, onRun: (String) -> Unit, onEdit: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (name in names) {
            OutlinedButton(
                onClick = { onRun(name) },
                enabled = enabled,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) { Text(name, fontWeight = FontWeight.Medium) }
        }
        TextButton(onClick = onEdit) { Text(if (names.isEmpty()) "+ Ajouter un favori" else "Modifier") }
    }
}

/** Mot de « autour » (voir QuestionFlow) → ce que montre la carte */
private val NEARBY_MODES = mapOf(
    "bus" to NearbyMode.BUS, "tram" to NearbyMode.TRAM, "metro" to NearbyMode.METRO, "velo" to NearbyMode.BIKE,
)

/** Logo de l'en-tête : l'icône de l'appli (tram blanc sur rouge), en carré aux coins arrondis */
@Composable
private fun AppLogo() {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(colorResource(R.color.icon_background)),
        contentAlignment = Alignment.Center,
    ) {
        // Le dessin occupe la zone sûre (66 sur 108) de l'icône adaptative : agrandi pour remplir le carré
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.requiredSize(52.dp))
    }
}

