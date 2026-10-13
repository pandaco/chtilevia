package fr.chtilevia.android

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import fr.chtilevia.application.FavoritesFile

/**
 * Favoris, sur toute la hauteur de l'écran : la liste, puis un favori (son nom, ses questions),
 * puis une question, tapée avec les mêmes propositions que la barre de saisie
 */
@Composable
fun FavoritesScreen(model: MainViewModel) {
    val draft = model.draft
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        when {
            draft != null && model.editedQuestion != null -> QuestionEditor(model, draft)
            draft != null -> DraftEditor(model, draft)
            else -> FavoritesList(model)
        }
    }
}

@Composable
private fun ColumnScope.FavoritesList(model: MainViewModel) {
    Header(title = "Favoris") {
        TextButton(onClick = { model.page = MainViewModel.Page.MAIN }) { Text("←  Retour") }
    }
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Hint(
                if (model.favoriteList.isEmpty()) {
                    "Aucun favori pour l'instant. Un favori pose une ou plusieurs questions d'un seul geste."
                } else {
                    "Touche un favori pour le modifier. L'ordre est celui des boutons de l'écran et du widget."
                },
            )
        }
        itemsIndexed(model.favoriteList, key = { _, favorite -> favorite.name }) { index, favorite ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { model.editFavorite(index) },
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(favorite.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        for (command in favorite.commands) {
                            Text(command, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (index > 0) {
                        IconButton(onClick = { model.moveUp(index) }) {
                            Icon(painterResource(R.drawable.ic_arrow_up), contentDescription = "Monter", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = "Modifier",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp).size(20.dp),
                    )
                }
            }
        }
    }
    BottomAction {
        Button(onClick = { model.draft = FavoriteDraft(null, "", emptyList()) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            IconLabel(R.drawable.ic_add, "Nouveau favori")
        }
    }
}

@Composable
private fun ColumnScope.DraftEditor(model: MainViewModel, draft: FavoriteDraft) {
    var confirmDelete by remember { mutableStateOf(false) }
    Header(title = if (draft.index == null) "Nouveau favori" else "Modifier le favori") {
        TextButton(onClick = { model.draft = null }) { Text("Annuler") }
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Label("Nom")
        OutlinedTextField(
            value = draft.name,
            onValueChange = { model.draft = draft.copy(name = it, error = null) },
            placeholder = { Text("Travail, Rentrer à vélo…") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Hint("C'est le texte du bouton, et ce que tu dis au micro pour le lancer.")

        Spacer(Modifier.height(8.dp))
        Label("Questions")
        Hint("Posées l'une après l'autre quand tu lances le favori. Touche une question pour la modifier.")
        draft.commands.forEachIndexed { index, command ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { model.editQuestion(index) },
            ) {
                Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(command, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
                    IconButton(onClick = { model.removeQuestion(index) }) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Retirer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        OutlinedButton(onClick = { model.editQuestion(MainViewModel.NEW_QUESTION) }, modifier = Modifier.fillMaxWidth()) {
            IconLabel(R.drawable.ic_add, if (draft.commands.isEmpty()) "Ajouter une question" else "Ajouter une autre question")
        }
        draft.error?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) }
        if (draft.index != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { confirmDelete = true }) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Supprimer ce favori")
            }
        }
    }
    BottomAction {
        Button(onClick = model::saveDraft, modifier = Modifier.fillMaxWidth().height(52.dp)) { IconLabel(R.drawable.ic_check, "Enregistrer") }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer « ${draft.name} » ?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    model.deleteDraft()
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Garder") } },
        )
    }
}

/** Une question du favori, avec l'étape et les propositions de la barre de saisie (sans « à proximité ») */
@Composable
private fun ColumnScope.QuestionEditor(model: MainViewModel, draft: FavoriteDraft) {
    val index = model.editedQuestion ?: return
    val initial = draft.commands.getOrNull(index)?.let { FavoritesFile.phrase(it) + " " }.orEmpty()
    var typed by rememberSaveable(index, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val field = remember { FocusRequester() }
    LaunchedEffect(model.focusField) {
        if (model.focusField) {
            runCatching { field.requestFocus() }
            model.focusField = false
        }
    }
    Header(title = draft.name.ifBlank { "Nouveau favori" }) {}
    TypingPanel(
        typed = typed,
        onChange = {
            typed = it
            model.questionError = null
            model.suggest(it.text, favorite = true)
        },
        suggestions = model.suggestions,
        field = field,
        enabled = true,
        onSend = { model.confirmQuestion(typed.text) },
        onClose = model::closeQuestion,
        placeholder = "tram croisé laroche vers roubaix…",
        sendIcon = R.drawable.ic_check,
        sendLabel = "Valider la question",
        error = model.questionError,
    )
}

/** En-tête des pages des favoris : une action à gauche (retour, annuler), puis le titre */
@Composable
private fun Header(title: String, action: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        action()
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}

@Composable
private fun BottomAction(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { content() }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
)
