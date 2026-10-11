# Contrat de comportement de chti

Données partagées par toutes les implémentations de chti (script shell aujourd'hui, cœur Kotlin et
serveur MCP demain) : mêmes entrées figées, mêmes sorties attendues. Une implémentation est
conforme quand elle reproduit **exactement** chaque fichier de `expected/`.

Ce dossier ne contient que des données. Chaque implémentation a son propre exécuteur, par exemple
`apps/shell/tests/contract.sh` pour le shell.

| Fichier | Rôle |
|---|---|
| `cases.txt` | Liste des cas : `nom \| heure \| flux temps réel \| arguments` |
| `clocks.txt` | Heures figées : `nom \| epoch \| date locale (pour information)` |
| `fixtures/gtfs.zip` | Extrait du GTFS : lignes 10, tram et M1, du 10 au 12/10/2026 |
| `fixtures/rt-midi.pb`, `rt-minuit.pb` | Flux temps réel enregistrés le 10/10/2026 à 12:46 et le 11/10 à 00:00 |
| `fixtures/station_*.json` | Quelques stations V'Lille, dont une hors service |
| `fixtures/config/chtilevia/favoris` | Favoris de test |
| `expected/<nom>.txt` | Sortie de référence de chaque cas |
| `make_fixtures.py` | Régénère les fixtures à partir d'un nouvel instantané |
| `schema/response.schema.json` | Schéma JSON (2020-12) des réponses de `chti --json` : format de réponse commun à toutes les façades (MCP, AppFunctions…) |

## Format des cas

Une ligne par cas, colonnes séparées par `|` (espaces autour ignorés), lignes `#` = commentaires :

```
bus-temps-reel | MIDI | rt-midi.pb | bus 10 marais
```

- **nom** : nom du fichier attendu dans `expected/` ;
- **heure** : un nom de `clocks.txt` (`MIDI`, `SOIR`, `MINUIT`) ;
- **flux temps réel** : un fichier de `fixtures/`, ou `aucun` pour simuler un flux indisponible
  (l'URL pointe alors vers un fichier absent) ;
- **arguments** : les arguments de `chti`, découpés sur les espaces (apostrophes autorisées).
  Seules les lignes 10, tram et M1 existent dans les fixtures.

## Conditions d'exécution

Pour chaque cas, l'exécuteur :

1. construit une fois l'index GTFS depuis `fixtures/gtfs.zip`, dans un cache temporaire ;
2. fige l'heure à l'epoch de la colonne **heure** ;
3. lit le GTFS, le flux temps réel et V'Lille dans `fixtures/`, les favoris dans
   `fixtures/config/chtilevia/favoris` ;
4. lance la commande et produit la sortie à comparer.

Pour le shell, cela passe par ces variables d'environnement :

| Variable | Valeur |
|---|---|
| `CHTI_NOW` | Epoch de l'heure figée |
| `CHTI_GTFS_URL` | `file://…/fixtures/gtfs.zip` |
| `CHTI_RT_URL` | `file://…/fixtures/<flux>` (ou un fichier absent pour `aucun`) |
| `CHTI_GBFS_URL` | `file://…/fixtures` |
| `XDG_CACHE_HOME` | Dossier temporaire |
| `XDG_CONFIG_HOME` | `…/fixtures/config` |

## Format des sorties attendues

- stdout et stderr fusionnés, dans l'ordre d'écriture ;
- lignes commençant par `curl: ` supprimées (le texte varie selon les versions de curl) ;
- chemin absolu de la racine du dépôt remplacé par `<racine>` ;
- dernière ligne : `[exit N]`, le code retour.

Les cas `json-*` produisent un document JSON (suivi de la ligne `[exit N]`) qui doit respecter
`schema/response.schema.json`.

## Ajouter un cas

1. Ajouter une ligne dans `cases.txt`.
2. Lancer `apps/shell/tests/contract.sh --update`.
3. Relire `expected/<nom>.txt` et vérifier que la sortie est juste : `--update` enregistre la sortie
   actuelle telle quelle, y compris si elle est fausse.
4. Commiter le cas avec sa référence.

Une nouvelle fonctionnalité commence par ses cas : ils fixent le comportement attendu de toutes les
implémentations.
