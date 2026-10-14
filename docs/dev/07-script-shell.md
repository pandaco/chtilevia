# 7. Le script shell

> **Dans ce chapitre :** comment `chti` tient en un seul fichier, sans aucune dépendance à
> installer, et comment le modifier sans le casser.

`apps/shell/chti` est l'implémentation historique et la plus portable : du sh POSIX, `curl`, `jq`,
`unzip` et Python **bibliothèque standard uniquement**. Il tourne sur macOS, sous Linux et dans
Termux.

## Organisation du script

| Partie | Rôle |
|---|---|
| sh — `usage`, `die`, `need`, `fetch` | Aide, erreurs, vérification des dépendances, téléchargements `curl` |
| sh — `gtfs_refresh` | Cache du GTFS (âge max 24 h), repli sur l'ancien cache si le réseau est KO (nouvel essai au plus une fois par heure) |
| sh — `transit`, `departures` | Lecture des arguments (arrêt, `-d`, `-n`), récupération du flux temps réel |
| sh — `velo` | V'Lille : `curl` + `jq` uniquement (jointure info / statut sur `station_id`) |
| python — `build_index` | Découpe `stop_times.txt` (~53 Mo) en un index par ligne (`idx/<route_id>.tsv`) |
| python — `pb`, `read_rt` | Décodeur protobuf minimal pour GTFS-Realtime |
| python — `rt_times` | Horaires temps réel d'une course, avec propagation du retard |
| python — `departures`, `list_stops` | Recherche d'arrêt et de direction, fusion théorique + temps réel, affichage |
| sh — `favorite_commands`, `run_favorite` | Lecture du fichier de favoris, enchaînement des commandes |
| python — `say_departures`, `spoken_line` | Sortie parlée (`--voix`) des passages ; la sortie parlée V'Lille est dans le filtre jq de `velo` |
| python — `parse_phrase` | `chti vocal` : phrase libre → arguments de `chti` |

Le Python est embarqué via un heredoc (`run_py`) et lancé en mode isolé (`python3 -I`).

## Autour du script

| Fichier | Rôle |
|---|---|
| `apps/shell/exemples/favoris` | Favoris d'exemple, copiés par l'installeur Termux |
| `apps/shell/termux/install.sh` | Installation dans Termux |
| `apps/shell/termux/widget.sh` | Devient la commande `chti-widget` : lance `chti --voix`, puis notification et, selon le mode, voix (`--parle`) ou message à l'écran (`--ecrit`) |
| `apps/shell/termux/shortcuts/*` | Les boutons du widget Termux (un fichier = un bouton) |
| `apps/shell/tests/*.sh` | Contrat, Termux simulé, reprise des anciens dossiers |

## shellcheck

Le shell est piégeux : une variable sans guillemets ou un nom de fichier avec un espace peuvent
faire planter le script dans certains cas seulement. `shellcheck` relit le script sans l'exécuter et
signale ces pièges, comme un correcteur orthographique.

```sh
npx nx run shell:lint
# ou directement :
shellcheck apps/shell/chti apps/shell/tests/*.sh apps/shell/termux/*.sh apps/shell/termux/shortcuts/*
```

S'il n'affiche rien, aucun problème n'a été trouvé. Un avertissement volontairement ignoré doit
être justifié en commentaire (`# shellcheck disable=SC2086 # raison`).

---

**Et ensuite →** [8. Le serveur MCP](08-serveur-mcp.md) : Chtipasse pour les assistants IA.
