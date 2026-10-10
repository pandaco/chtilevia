# ilevia-live

Prochains passages du réseau Ilévia (bus, tram, métro — Métropole de Lille) et disponibilité des
stations V'Lille, directement dans le terminal.

## Présentation

`ilevia` est un script shell unique, sans interface graphique, qui interroge les données ouvertes du réseau :

- **bus / tram** : prochains passages en temps réel à un arrêt, regroupés par direction ;
- **métro** : prochains passages théoriques (aucun temps réel métro n'est publié en open data) ;
- **V'Lille** : nombre de vélos disponibles et de places libres d'une station.

```
$ ilevia bus 10 marais -d flandres

10 — Marais  (LOMME MARAIS 59160 LOMME)
  → Lille Gare Lille Flandres
     12:53  dans 6 min      ⏱  temps réel
     13:15  dans 29 min     ⏱  temps réel
     13:22  dans 36 min     🕒 théorique

$ ilevia velo rihour
Rihour  (32 bornes)
  🚲 23 vélo(s) dispo   🅿️  9 place(s) libre(s)
  maj il y a 1 min
```

### Installation

Dépendances : `curl`, `jq`, `unzip`, `python3` (bibliothèque standard uniquement).

```sh
chmod +x ilevia
ln -s "$PWD/ilevia" ~/.local/bin/ilevia   # optionnel, pour l'avoir dans le PATH
```

## Appels possibles

| Commande | Effet |
|---|---|
| `ilevia bus <ligne>` | Liste les arrêts de la ligne, dans chaque sens |
| `ilevia bus <ligne> <arrêt>` | Prochains passages à l'arrêt, toutes directions |
| `ilevia bus <ligne> <arrêt> -d <direction>` | Prochains passages à l'arrêt, vers cette direction uniquement |
| `ilevia bus <ligne> <arrêt> -n <nombre>` | Affiche `<nombre>` passages par direction au lieu de 3 |
| `ilevia bus <ligne> -d <direction>` | Liste des arrêts, pour cette direction uniquement |
| `ilevia tram [arrêt] [-d <direction>] [-n <nombre>]` | Idem pour le tramway (Lille ↔ Roubaix / Tourcoing) |
| `ilevia metro <1\|2> [arrêt] [-d <direction>] [-n <nombre>]` | Idem pour le métro M1 ou M2 (horaires théoriques) |
| `ilevia velo <station>` | Vélos disponibles et places libres des stations V'Lille correspondantes |
| `ilevia update` | Force le re-téléchargement du référentiel GTFS |
| `ilevia help` | Affiche l'aide |

### Paramètres

- **`<ligne>`** : nom de la ligne tel qu'affiché sur le réseau — `10`, `L1`, `C5`, `CO3`, `Z2`… (insensible à la casse).
- **`<arrêt>`** / **`<station>`** : tout ou partie du nom, sur un ou plusieurs mots, sans guillemets nécessaires.
  Casse, accents et ponctuation sont ignorés. Si plusieurs arrêts correspondent (ex. « gambetta »
  existe dans plusieurs communes), chacun est affiché avec sa commune.
- **`-d <direction>`** (alias `--direction`, `--vers`, ou `--vers=<direction>`) : ne garde que les
  courses dont la destination contient ce texte. Placée n'importe où après la ligne.
  Une direction inconnue affiche la liste des directions possibles.
- **`-n <nombre>`** (alias `--nombre`, ou `--nombre=<nombre>`) : nombre de prochains passages affichés
  par direction, entier ≥ 1, **3 par défaut**. Si le service du jour ne suffit pas, les premiers
  départs du lendemain sont affichés. Combinable avec `-d`, dans n'importe quel ordre.
- **`metro`** accepte `1`, `2`, `m1`, `m2` ; `métro`, `vélo` et `vlille` sont acceptés comme alias.

### Exemples

```sh
ilevia bus 10                          # arrêts de la ligne 10
ilevia bus 10 gambetta                 # passages à « Gambetta », dans tous les sens
ilevia bus l1 republique -d wambrechies  # passages de la Liane 1 vers Wambrechies
ilevia tram croise laroche --vers roubaix
ilevia tram -d tourcoing               # arrêts du tram vers Tourcoing
ilevia metro 1 republique -d chu
ilevia metro 2 gare lille flandres -n 10   # les 10 prochaines rames dans chaque sens
ilevia bus 10 gambetta -d flandres -n 5    # les 5 prochains bus vers Lille Flandres
ilevia velo gare                       # toutes les stations V'Lille contenant « gare »
```

### Lecture du résultat

- Les N prochains passages par direction (`-n`, 3 par défaut) ; au-delà d'une heure, l'attente
  s'affiche en heures (`dans 8 h 04`).
- `⏱  temps réel` : horaire issu du suivi des véhicules ; `🕒 théorique` : horaire planifié.
- Code retour `0` en cas de succès, `1` si ligne / arrêt / direction / station introuvable ou si le service est injoignable.

## Sources de données (open data, sans clé d'API)

| Donnée | URL | Format |
|---|---|---|
| Temps réel bus + tram | `https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt` | GTFS-RT (protobuf) |
| Référentiel lignes / arrêts / horaires | `https://media.ilevia.fr/opendata/gtfs.zip` | GTFS |
| Stations V'Lille | `https://media.ilevia.fr/opendata/station_information.json` | GBFS 2.3 |
| Disponibilité V'Lille | `https://media.ilevia.fr/opendata/station_status.json` | GBFS 2.3 |

Le GTFS (~9 Mo) est mis en cache dans `~/.cache/ilevia` (ou `$XDG_CACHE_HOME/ilevia`) et rafraîchi
automatiquement toutes les 24 h.

### Requêtes curl brutes

V'Lille :

```sh
# Toutes les stations (id, nom, capacité, coordonnées)
curl -s https://media.ilevia.fr/opendata/station_information.json | jq '.data.stations[]'

# Retrouver l'id d'une station par son nom
curl -s https://media.ilevia.fr/opendata/station_information.json \
  | jq '.data.stations[] | select(.name | test("rihour"; "i")) | {station_id, name, capacity}'

# Disponibilité d'une station (ici id 10 = Rihour)
curl -s https://media.ilevia.fr/opendata/station_status.json \
  | jq '.data.stations[] | select(.station_id == "10") | {num_bikes_available, num_docks_available, is_renting, is_returning}'
```

Bus, tram, métro :

```sh
# Référentiel GTFS : routes.txt (lignes), stops.txt (arrêts), trips.txt, stop_times.txt (horaires), calendar_dates.txt
curl -sL -o gtfs.zip https://media.ilevia.fr/opendata/gtfs.zip && unzip -o gtfs.zip -d gtfs

# Flux temps réel (binaire protobuf GTFS-RT — décodé par le script)
curl -s -o ilevia.pb https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt
```

Le flux temps réel est du protobuf binaire : il n'est pas lisible avec `jq`, d'où le petit décodeur
Python embarqué dans `ilevia`.

### Limites

- **Métro (M1, M2)** : aucun temps réel publié en open data ; passages théoriques uniquement.
- **Bus / tram** : le flux temps réel ne détaille que les prochains arrêts de chaque course ; pour les
  arrêts suivants, le dernier retard connu est propagé (comportement standard GTFS-RT). Les courses
  sans suivi temps réel sont affichées en théorique.
- Sans réseau, le script retombe sur les horaires théoriques du cache ; V'Lille nécessite le réseau.

## Contribution

### Organisation du script

Tout tient dans le fichier `ilevia` :

| Partie | Rôle |
|---|---|
| sh — `usage`, `die`, `need`, `fetch` | Aide, erreurs, vérification des dépendances, téléchargements `curl` |
| sh — `gtfs_refresh` | Cache du GTFS (âge max 24 h), repli sur l'ancien cache si le réseau est KO (nouvel essai au plus une fois par heure) |
| sh — `transit`, `departures` | Parsing des arguments (arrêt, `-d`, `-n`), récupération du flux temps réel |
| sh — `velo` | V'Lille : `curl` + `jq` uniquement (jointure info / statut sur `station_id`) |
| python — `build_index` | Découpe `stop_times.txt` (~53 Mo) en un index par ligne (`idx/<route_id>.tsv`) |
| python — `pb`, `read_rt` | Décodeur protobuf minimal pour GTFS-Realtime |
| python — `rt_times` | Horaires temps réel d'une course, avec propagation du retard |
| python — `departures`, `list_stops` | Recherche d'arrêt / direction, fusion théorique + temps réel, affichage |

Le Python est embarqué via un heredoc (`run_py`) et lancé en mode isolé (`python3 -I`).

### Conventions

- Aucune dépendance à installer : sh POSIX, `curl`, `jq`, `unzip`, Python **bibliothèque standard uniquement**.
- Commentaires en français, identifiants (variables, fonctions) en anglais.
- Messages utilisateur en français, erreurs sur stderr via `die`, code retour non nul en cas d'échec.
- Fichiers de travail non versionnés dans `./tmp` (ignoré par git).

### Tests

```sh
tests/run.sh                      # tests de non-régression (hors ligne, ~5 s)
shellcheck ilevia tests/run.sh    # paquet shellcheck
```

Les tests n'utilisent pas le réseau. Ils lancent `ilevia` sur des **fixtures figées**
(`tests/fixtures/`) à une **heure figée**, puis comparent stdout, stderr et le code retour à
`tests/expected/<cas>.txt`. Ils couvrent le temps réel avec propagation du retard, le passage de minuit,
le service du lendemain, l'absence de flux temps réel, les filtres `-d` / `-n`, V'Lille et les erreurs.

| Fichier | Rôle |
|---|---|
| `tests/run.sh` | Liste des cas (`nom \| heure \| flux temps réel \| arguments`) et comparaison des sorties |
| `tests/fixtures/` | Sous-ensemble du GTFS (lignes 10, tram, M1 ; 10–12/10/2026), 2 instantanés GTFS-RT (12:46 et 00:00), V'Lille |
| `tests/expected/` | Sorties attendues |
| `tests/make_fixtures.py` | Régénère les fixtures à partir d'un instantané brut des flux |

**Ajouter un cas** : ajouter une ligne dans `CASES` de `tests/run.sh`, lancer `tests/run.sh --update`,
puis **relire** le fichier `tests/expected/<cas>.txt` généré avant de le commiter : `--update`
enregistre la sortie actuelle telle quelle, y compris si elle est fausse.

Variables d'environnement utilisées par les tests (utilisables aussi à la main) :

| Variable | Effet |
|---|---|
| `ILEVIA_NOW` | Heure courante figée (timestamp epoch) |
| `ILEVIA_GTFS_URL`, `ILEVIA_RT_URL`, `ILEVIA_GBFS_URL` | Sources de données (acceptent `file://`) |
| `XDG_CACHE_HOME` | Emplacement du cache (`$XDG_CACHE_HOME/ilevia`) |

Pour tester à la main le comportement hors ligne : `HTTPS_PROXY=http://127.0.0.1:9 ./ilevia bus 10 gambetta`
(le script doit retomber sur les horaires théoriques).

### Commits

Messages en anglais, au format Conventional Commits compact, un commit par changement logique :

```text
feat: add direction filter for transit commands

Users at busy stops only care about one direction; filtering on the
trip headsign keeps the output short.
```
