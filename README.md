# chtilevia

> T'as ton bus, biloute ?

Prochains passages du réseau Ilévia (bus, tram, métro — Métropole Européenne de Lille) et
disponibilité des stations V'Lille, directement dans le terminal, avec une seule commande : `chti`.

> **Projet indépendant, non affilié à Ilévia ni à la MEL.** Les données proviennent de l'open data
> publié par Ilévia et transport.data.gouv.fr ; aucune garantie d'exactitude.

## Présentation

`chti` est un script shell unique, sans interface graphique, qui interroge les données ouvertes du réseau :

- **bus / tram** : prochains passages en temps réel à un arrêt, regroupés par direction ;
- **métro** : prochains passages théoriques (aucun temps réel métro n'est publié en open data) ;
- **V'Lille** : nombre de vélos disponibles et de places libres d'une station.

```
$ chti bus 10 marais -d flandres

10 — Marais  (LOMME MARAIS 59160 LOMME)
  → Lille Gare Lille Flandres
     12:53  dans 6 min      ⏱  temps réel
     13:15  dans 29 min     ⏱  temps réel
     13:22  dans 36 min     🕒 théorique

$ chti velo rihour
Rihour  (32 bornes)
  🚲 23 vélo(s) dispo   🅿️  9 place(s) libre(s)
  maj il y a 1 min
```

### Installation

Dépendances : `curl`, `jq`, `unzip`, `python3` (bibliothèque standard uniquement).

```sh
chmod +x apps/shell/chti
ln -s "$PWD/apps/shell/chti" ~/.local/bin/chti   # optionnel, pour l'avoir dans le PATH
```

## Appels possibles

| Commande | Effet |
|---|---|
| `chti bus <ligne>` | Liste les arrêts de la ligne, dans chaque sens |
| `chti bus <ligne> <arrêt>` | Prochains passages à l'arrêt, toutes directions |
| `chti bus <ligne> <arrêt> -d <direction>` | Prochains passages à l'arrêt, vers cette direction uniquement |
| `chti bus <ligne> <arrêt> -n <nombre>` | Affiche `<nombre>` passages par direction au lieu de 3 |
| `chti bus <ligne> -d <direction>` | Liste des arrêts, pour cette direction uniquement |
| `chti tram [arrêt] [-d <direction>] [-n <nombre>]` | Idem pour le tramway (Lille ↔ Roubaix / Tourcoing) |
| `chti metro <1\|2> [arrêt] [-d <direction>] [-n <nombre>]` | Idem pour le métro M1 ou M2 (horaires théoriques) |
| `chti velo <station>` | Vélos disponibles et places libres des stations V'Lille correspondantes |
| `chti velo <station> --prendre` | Seulement les vélos disponibles (pour partir) |
| `chti velo <station> --deposer` | Seulement les places libres (pour rendre le vélo) |
| `chti autour <latitude> <longitude> [-n <nombre>]` | Arrêts les plus proches (avec leurs lignes) et stations V'Lille les plus proches, à vol d'oiseau |
| `chti <favori>` | Lance la ou les commandes enregistrées sous ce nom (voir [Favoris](#favoris)) |
| `chti favoris` | Liste les favoris |
| `chti --voix <commande>` | Même réponse, en phrases à lire à voix haute (voir [Sortie parlée](#sortie-parlée)) |
| `chti --json <commande>` | Même réponse, en JSON pour les programmes (voir [Sortie JSON](#sortie-json)) |
| `chti vocal "<phrase>"` | Comprend une phrase libre et répond en mode voix (voir [Phrases comprises](#phrases-comprises)) |
| `chti update` | Force le re-téléchargement du référentiel GTFS |
| `chti help` | Affiche l'aide |

### Paramètres

- **`<ligne>`** : nom de la ligne tel qu'affiché sur le réseau — `10`, `L1`, `C5`, `CO3`, `Z2`… (insensible à la casse).
- **`<arrêt>`** / **`<station>`** : tout ou partie du nom, sur un ou plusieurs mots, sans guillemets nécessaires.
  Casse, accents et ponctuation sont ignorés. Si plusieurs arrêts correspondent (ex. « gambetta »
  existe dans plusieurs communes), chacun est affiché avec sa commune.
  Si aucun arrêt ne contient le texte, chti prend celui qui lui ressemble le plus, à l'écrit ou au
  son : fautes de frappe ou de reconnaissance vocale (`gambeta`, `croix la roche`, `saint mort`),
  mots collés ou séparés, mot manquant (`eglise marais`). Si plusieurs arrêts ressemblent autant à
  la demande, chti n'en choisit aucun et les propose (code `ambiguous_stop`).
- **`-d <direction>`** (alias `--direction`, `--vers`, ou `--vers=<direction>`) : ne garde que les
  courses dont la destination contient ce texte (à défaut, celles dont la destination lui ressemble
  le plus). Placée n'importe où après la ligne.
  Une direction inconnue affiche la liste des directions possibles ; un arrêt qui existe mais n'est
  pas desservi dans cette direction le dit, avec les directions qui le desservent.
  Le mot **`vers`** sans tiret marche aussi : tous les mots qui le suivent forment la direction
  (`chti bus 10 gambetta vers lille flandres`).
- **`-n <nombre>`** (alias `--nombre`, ou `--nombre=<nombre>`) : nombre de prochains passages affichés
  par direction, entier ≥ 1, **3 par défaut**. Si le service du jour ne suffit pas, les premiers
  départs du lendemain sont affichés. Combinable avec `-d`, dans n'importe quel ordre.
- **`<latitude> <longitude>`** (pour `autour`) : en degrés décimaux, comme les donne une appli de
  carte : `50.6366 3.0630`, `50.6366, 3.0630`, `50.6366,3.0630` ou `50,6366 3,0630`. Avec `-n`,
  le nombre d'arrêts et de stations donnés (3 par défaut).
- **`--prendre`** / **`--deposer`** (ou `--déposer`) : pour V'Lille, n'affiche que les vélos
  disponibles, ou que les places libres. Indique aussi « location / dépôt impossible » si la station
  le refuse.
- **`metro`** accepte `1`, `2`, `m1`, `m2` ; `métro`, `vélo` et `vlille` sont acceptés comme alias.

### Exemples

```sh
chti bus 10                          # arrêts de la ligne 10
chti bus 10 gambetta                 # passages à « Gambetta », dans tous les sens
chti bus l1 republique -d wambrechies  # passages de la Liane 1 vers Wambrechies
chti tram croise laroche --vers roubaix
chti tram -d tourcoing               # arrêts du tram vers Tourcoing
chti metro 1 republique -d chu
chti metro 2 gare lille flandres -n 10   # les 10 prochaines rames dans chaque sens
chti bus 10 gambetta -d flandres -n 5    # les 5 prochains bus vers Lille Flandres
chti velo gare                       # toutes les stations V'Lille contenant « gare »
chti tram croise laroche vers roubaix  # « vers » sans tiret
chti travail                         # un favori
```

### Favoris

Un favori donne un nom court à une ou plusieurs commandes : `chti travail` au lieu de
`chti tram croise laroche vers roubaix -n 2`. Ils sont lus dans `~/.config/chtilevia/favoris`
(ou `$XDG_CONFIG_HOME/chtilevia/favoris`), au format `nom: commande` :

```
# Le tram pour aller au travail
travail:      tram croise laroche vers roubaix -n 2

# Rentrer à vélo : vélos au départ, places à l'arrivée (un nom répété = commandes enchaînées)
rentrer-velo: velo gare lille europe --prendre
rentrer-velo: velo la madeleine gare --deposer
```

- Un fichier d'exemple est fourni : `mkdir -p ~/.config/chtilevia && cp apps/shell/exemples/favoris ~/.config/chtilevia/`.
- La commande est celle que tu taperais après `chti`. `#` commence un commentaire.
- Le nom ignore la casse ; il ne doit pas reprendre une commande existante (`bus`, `velo`…).
- Si une des commandes échoue, les suivantes sont quand même lancées et le code retour vaut `1`.
- Attention aux directions trop larges : pour le bus 10, « vers lille » correspond aux deux sens
  (Lille Flandres et Lille CHU). Vérifie avec `chti bus 10 -d lille`.

### Lecture du résultat

- Les N prochains passages par direction (`-n`, 3 par défaut) ; au-delà d'une heure, l'attente
  s'affiche en heures (`dans 8 h 04`).
- `⏱  temps réel` : horaire issu du suivi des véhicules ; `🕒 théorique` : horaire planifié.
- Code retour `0` en cas de succès, `1` si ligne / arrêt / direction / station / favori introuvable ou si le service est injoignable.

### Sortie parlée

`--voix` (placé n'importe où) remplace l'affichage par des phrases simples, sans emoji ni colonnes,
pensées pour la synthèse vocale. Il sert au [widget Android](#sur-android), mais
marche partout :

```
$ chti --voix bus 10 marais vers flandres
Bus 10, arrêt Marais, vers Lille Gare Lille Flandres : dans 7 minutes, puis dans 30 minutes.

$ chti --voix metro 1 republique -d chu
Métro 1, arrêt République Beaux-Arts, vers Lille Chu - Eurasanté : dans 1 minute, puis dans 4 minutes. Horaires théoriques.

$ chti --voix rentrer-velo
Gare Lille Europe : 14 vélos disponibles.
La Madeleine Gare : 13 places libres.
```

- 2 passages par direction par défaut (`-n` pour en demander plus).
- Au-delà d'une heure, l'heure est donnée (« à 7 h 42 »). Un horaire sans temps réel est suivi de
  « (prévu) », ou la phrase se termine par « Horaires théoriques. » s'ils le sont tous.
- Les erreurs sont aussi des phrases, sur stderr, sans le préfixe `chti :`.

### Sortie JSON

`--json` (placé n'importe où) remplace l'affichage par un document JSON unique sur stdout, pour les
programmes : serveur MCP pour les assistants IA, scripts, widgets. Sa forme est décrite par le
schéma [`libs/contract/schema/response.schema.json`](libs/contract/schema/response.schema.json).

```
$ chti --json bus 10 marais -d flandres -n 1
{
  "type": "departures",
  "line": { "id": "10", "name": "10", "mode": "bus", "label": "Bus 10", … },
  "now": "2026-10-10T12:46:00+02:00",
  "stops": [
    { "id": "1008", "name": "Marais", "description": "LOMME MARAIS 59160 LOMME",
      "directions": [
        { "headsign": "Lille Gare Lille Flandres",
          "departures": [ { "time": "2026-10-10T12:53:01+02:00", "minutes": 7, "realtime": true } ] } ] }
  ],
  "warnings": []
}
```

- `type` donne la forme du document : `departures`, `stops` (ligne sans arrêt), `bike_stations`,
  `favorites`, `favorite` (un résultat par commande du favori), `update` ou `error`.
- Les erreurs sont aussi du JSON sur stdout (`"type": "error"`), avec un `code` stable
  (`unknown_line`, `unknown_direction`…), le `message` en français et parfois des `details`
  (directions possibles). Le code retour reste `1`.
- Les avertissements (flux temps réel indisponible, GTFS non mis à jour) sont dans `warnings`, pas
  sur stderr.
- Pour V'Lille, le JSON donne les faits bruts (vélos, places, location / dépôt possibles) ;
  `--prendre` / `--deposer` sont seulement rappelés dans `mode`.
- Incompatible avec `--voix` et avec `vocal` : un programme interprète la phrase lui-même.

### Phrases comprises

`chti vocal "<phrase>"` transforme une phrase dite naturellement (telle que la renvoie la
reconnaissance vocale) en commande `chti`, puis répond en [sortie parlée](#sortie-parlée) :

| Phrase | Commande lancée |
|---|---|
| « Prochain bus 10 à Gambetta vers Lille Flandres » | `bus 10 gambetta -d lille flandres` |
| « Quand passe le bus dix à l'arrêt Marais direction Lille Flandres » | `bus 10 marais -d lille flandres` |
| « Métro un République vers CHU » | `metro 1 republique -d chu` |
| « Le prochain tram à Croisé Laroche vers Roubaix » | `tram croise laroche -d roubaix` |
| « Liane 1 République » | `bus L1 republique` |
| « V'Lille Gare Lille Flandres pour déposer » | `velo gare lille flandres --deposer` |
| « Vélo gare Lille Europe pour prendre un vélo » | `velo gare lille europe --prendre` |
| « Travail », « Rentrer à vélo » | le favori `travail`, `rentrer-velo` |
| « Vélo à proximité », « les arrêts autour de moi », « le métro le plus près d'ici » | `autour`, complété de la position par l'appli Android |

Règles :

- **Mode** : `bus` / `ligne` / `liane`, `métro`, `tram` / `tramway`, `vélo` / `V'Lille`, ou un nom de favori
  (les petits mots comme « à » sont ignorés : « rentrer à vélo » trouve `rentrer-velo`).
- **Ligne** : en chiffres ou en lettres jusqu'à vingt (« dix » → `10`) ; « liane 1 » → `L1`.
- **Direction** : tout ce qui suit « vers », « direction » ou « en direction de ».
- **V'Lille** : « prendre » / « emprunter » → `--prendre` ; « déposer » / « rendre » / « poser » → `--deposer`.
- **À proximité** : « proximité », « proche », « autour de moi », « près d'ici »… → `autour`. Le shell
  ne connaît pas la position du téléphone et demande donc une position ; l'appli Android ajoute la
  sienne (avec ton accord).
- Les mots d'introduction (« quand passe le prochain… », « à l'arrêt ») sont ignorés en début de
  phrase et en début de nom d'arrêt, mais jamais au milieu d'un nom (« Pont de Bois » reste intact).
- Si la phrase n'est pas comprise, la réponse le dit et donne un exemple (code retour `1`).

## Sur Android

Sur un téléphone Android, chti tourne directement sur le téléphone, **sans serveur**, de deux façons :

- **l'appli chtilevia** (`apps/android`, cœur Kotlin) : un écran et un widget d'accueil, sans rien
  d'autre à installer ;
- **Termux** (`apps/shell/termux`) : le script `chti` et le widget Termux:Widget.

Dans les deux cas, le widget propose deux façons de demander, et la réponse suit la façon de demander :

- **à la voix** : le bouton *Parler* écoute ta question et **lit la réponse à voix haute** ;
- **en un clic** : un bouton de favori **affiche la réponse**, sans voix.

| Bouton du widget | Ce qu'il fait | Réponse |
|---|---|---|
| **Parler** | Écoute une phrase (« métro 1 République vers CHU », « vélo à proximité », « travail »…) | À voix haute + notification |
| **Travail** | Favori `travail` | Écrite + notification |
| **Rentrer à vélo** | Favori `rentrer-velo` : vélos au départ, places à l'arrivée | Écrite + notification |

Un favori dit à voix haute (« travail » via *Parler*) répond donc à la voix ; le même favori touché
sur son bouton répond par écrit. Les phrases comprises sont décrites dans
[Phrases comprises](#phrases-comprises).

> Pourquoi pas l'appli Gemini ? Elle ne peut appeler un outil personnel (« custom app ») que pour
> les comptes aux États-Unis, en anglais, et seulement via un serveur. Le widget est la seule voie
> vocale sans serveur depuis la France.

### Appli chtilevia

```sh
npx nx run :android:assembleDebug                                          # construit l'appli
adb install -r apps/android/build/outputs/apk/debug/android-debug.apk      # téléphone en débogage USB ou Wi-Fi
```

- **Écran** : gros bouton 🎤, question au clavier en secours, réponse affichée et lue, favoris en
  boutons, éditeur de favoris (même format que le fichier du shell : `nom: commande`).
- **Widget** : appui long sur l'accueil → *Widgets* → *chtilevia*. Ses boutons ouvrent une carte en
  bas de l'écran, par-dessus l'accueil, qui reste jusqu'à ce qu'on la touche ; la dernière réponse
  est aussi gardée en notification.
- **À proximité** : « vélo à proximité » utilise la position du téléphone (autorisation demandée la
  première fois) et répond comme `chti autour`.
- **Raccourcis** : appui long sur l'icône → *Parler* et un raccourci par favori.
- **Horaires** : téléchargés au premier lancement (~8 s), puis mis à jour chaque jour en arrière-plan
  (en Wi-Fi, batterie suffisante), et gardés en mémoire tant que l'appli tourne (réponse en moins
  d'un dixième de seconde ensuite). 44 Mo sur le téléphone.

### Termux

**👉 Installation et utilisation pas à pas : [docs/android.md](docs/android.md)**. Le guide couvre les
applis à installer (Termux, Termux:API, Termux:Widget depuis F-Droid), la copie du projet sans le
publier, l'installeur, les favoris, l'ajout du widget, les phrases comprises, l'ajout de boutons,
la mise à jour, le dépannage et la désinstallation.

En résumé :

```sh
# dans Termux, une fois le projet copié dans ~/chtilevia
sh ~/chtilevia/apps/shell/termux/install.sh   # outils, commandes, boutons du widget, horaires
nano ~/.config/chtilevia/favoris       # tes trajets (fichier local, jamais publié)
```

## Avec un assistant IA (MCP)

Un serveur [MCP](https://modelcontextprotocol.io) local donne les mêmes informations à Claude Code,
Claude Desktop, Gemini CLI ou tout client MCP : on pose la question en langage naturel, l'assistant
appelle `prochains_passages`, `station_velo`, `favori`… Il s'appuie sur le cœur Kotlin compilé pour
Node (mêmes réponses que `chti --json`, sans curl, jq ni python3) et tourne en stdio, sans serveur à
héberger.

```sh
npm install
npx nx run :node:build                                                               # cœur Kotlin pour Node
claude mcp add --scope user chtilevia -- node "$PWD/apps/mcp/src/main.ts"           # Claude Code
gemini mcp add -s user chtilevia node "$PWD/apps/mcp/src/main.ts"                   # Gemini CLI
```

Outils, configuration de Claude Desktop et architecture : [`apps/mcp/README.md`](apps/mcp/README.md).

## Sources de données (open data, sans clé d'API)

| Donnée | URL | Format |
|---|---|---|
| Temps réel bus + tram | `https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt` | GTFS-RT (protobuf) |
| Référentiel lignes / arrêts / horaires | `https://media.ilevia.fr/opendata/gtfs.zip` | GTFS |
| Stations V'Lille | `https://media.ilevia.fr/opendata/station_information.json` | GBFS 2.3 |
| Disponibilité V'Lille | `https://media.ilevia.fr/opendata/station_status.json` | GBFS 2.3 |

Le GTFS (~9 Mo) est mis en cache dans `~/.cache/chtilevia` (ou `$XDG_CACHE_HOME/chtilevia`) et rafraîchi
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
Python embarqué dans `chti`.

### Limites

- **Métro (M1, M2)** : aucun temps réel publié en open data ; passages théoriques uniquement.
- **Bus / tram** : le flux temps réel ne détaille que les prochains arrêts de chaque course ; pour les
  arrêts suivants, le dernier retard connu est propagé (comportement standard GTFS-RT). Les courses
  sans suivi temps réel sont affichées en théorique.
- Sans réseau, le script retombe sur les horaires théoriques du cache ; V'Lille nécessite le réseau.
- Android : pas de mot d'activation (« Hey Google ») : il faut toucher le widget. L'installation n'a pas encore été validée sur un vrai
  téléphone, seulement en simulant les commandes Termux (`apps/shell/tests/termux.sh`).

## Contribution

### Organisation du script

L'essentiel tient dans le fichier `chti` :

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
| sh — `favorite_commands`, `run_favorite` | Lecture du fichier de favoris, enchaînement des commandes |
| python — `say_departures`, `spoken_line` | Sortie parlée (`--voix`) des passages ; la sortie parlée V'Lille est dans le filtre jq de `velo` |
| python — `parse_phrase` | `chti vocal` : phrase libre → arguments de `chti` |

Le Python est embarqué via un heredoc (`run_py`) et lancé en mode isolé (`python3 -I`).

Autour du script :

| Fichier | Rôle |
|---|---|
| `apps/shell/exemples/favoris` | Favoris d'exemple, copiés par l'installeur Android |
| `apps/shell/termux/install.sh` | Installation dans Termux |
| `apps/shell/termux/widget.sh` | Devient la commande `chti-widget` : lance `chti --voix`, puis notification et, selon le mode, voix (`--parle`) ou message à l'écran (`--ecrit`) |
| `apps/shell/termux/shortcuts/*` | Les boutons du widget (un fichier = un bouton) |
| `docs/android.md` | Guide d'installation et d'utilisation sur Android |

### Conventions

- Aucune dépendance à installer : sh POSIX, `curl`, `jq`, `unzip`, Python **bibliothèque standard uniquement**.
- Commentaires en français, identifiants (variables, fonctions) en anglais.
- Messages utilisateur en français, erreurs sur stderr via `die`, code retour non nul en cas d'échec.
- Fichiers de travail non versionnés dans `./tmp` (ignoré par git).

### Tests

Deux outils vérifient le script. Ils ne servent pas à l'usage quotidien, seulement **après
chaque modification**, pour s'assurer qu'on n'a rien cassé.

```sh
apps/shell/tests/contract.sh   # tests de non-régression : 79 cas, hors ligne, ~7 s
apps/shell/tests/termux.sh     # partie Android, avec des commandes Termux simulées : 19 vérifications
shellcheck apps/shell/chti apps/shell/tests/*.sh apps/shell/termux/*.sh apps/shell/termux/shortcuts/*   # analyse du shell (paquet shellcheck)
```

**Avant chaque commit**, tout doit passer : les deux scripts de test doivent afficher `0 échec(s)`,
et `shellcheck` ne doit rien afficher.

Le dépôt est un monorepo [Nx](https://nx.dev) : la même vérification se lance en une commande pour
tous les projets, avec un cache qui saute ce qui n'a pas changé (Node requis) :

```sh
npm install                          # une fois : installe Nx dans node_modules/
npx nx run-many -t lint test build   # shellcheck, typage, tests et compilation de tous les projets
npx nx graph                         # graphe des projets et de leurs dépendances
```

`libs/contract` ne contient que des données : fixtures et sorties de référence, partagées par toutes
les implémentations de chti (script shell, serveur MCP, cœur Kotlin).

#### Cœur Kotlin (`libs/`)

La logique de chti est portée en Kotlin Multiplatform, en architecture hexagonale, pour l'appli
Android et le serveur MCP. Le cœur se comporte exactement comme le script : il passe les mêmes cas
du contrat (`libs/contract`), au caractère près. Chaque module compile pour la JVM, Android et Node.js.

| Module | Rôle | Dépend de |
|---|---|---|
| `libs/domain` | Modèles et règles métier purs : recherche sans accents et approximative, jours de service, fusion théorique / temps réel | rien (sauf kotlinx-datetime) |
| `libs/application` | Cas d'usage, ports (GTFS, temps réel, V'Lille, favoris, horloge), grammaire des commandes, phrase libre | domain |
| `libs/presentation` | Sorties texte, voix et JSON du contrat ; `Chti`, équivalent de la commande | application |
| `libs/adapters` | GTFS statique (cache partagé avec le shell), GTFS-RT, V'Lille (GBFS), fichier des favoris | application |
| `libs/node` | Racine de composition pour Node : `runChti`, la commande exportée en module ES pour le serveur MCP | presentation, adapters |
| `libs/core-tests` | Règles d'architecture (Konsist) et contrat joué par le cœur | tout |

Les adaptateurs sont communs à toutes les cibles (décodage CSV, protobuf, JSON, zip ; cache GTFS).
Seul l'accès au système est propre à chaque plateforme : `java.io` / `java.net` sur la JVM et Android
(source set `jvmShared`), modules `node:*` sous Node (le téléchargement passe par un processus `node`
enfant, pour garder des ports synchrones).

Prérequis : un JDK 17 ou plus récent (`JAVA_HOME`) et le SDK Android (`ANDROID_HOME`, installé avec
Android Studio). Gradle (wrapper `./gradlew`) est lancé par Nx ; on peut aussi l'appeler directement :

```sh
./gradlew test                       # tests de tous les modules, sur toutes les cibles
./gradlew :domain:jvmTest            # une seule cible
./gradlew :core-tests:test           # architecture et contrat
```

- La configuration commune des modules est dans `buildSrc/` (plugin `chtilevia.kmp-library`) et les
  versions dans `gradle/libs.versions.toml`.
- `libs/core-tests` fait échouer le build si une couche importe ce qu'elle ne devrait pas (le cœur
  n'importe que la bibliothèque standard et kotlinx-datetime, la présentation pas les adaptateurs…).
- Le contrat crée un test par cas de `libs/contract/cases.txt` : `Chti` est lancé sur les fixtures,
  à l'heure du cas, et sa sortie doit être identique à `expected/<cas>.txt`.
- Nx ne met pas en cache les tâches Gradle : les entrées qu'il en déduit ne suivent pas les
  dépendances (une sortie d'avant un changement de version pouvait être restaurée). Gradle a son
  propre cache et sait ce qui est à jour.

#### Pourquoi des tests « figés » ?

Les horaires changent chaque minute : en lançant `chti bus 10 marais` à la main, impossible de
savoir si « 12:53 dans 7 min » est juste ou faux, puisque la bonne réponse change sans arrêt.
Les tests figent donc le monde :

1. **Données figées** : une copie des flux enregistrée à un instant précis (`libs/contract/fixtures/`).
2. **Heure figée** : le script croit qu'on est à cet instant-là (variable `CHTI_NOW`).
3. **Réponses de référence** : la sortie correcte de chaque commande, relue et enregistrée une fois
   pour toutes (`libs/contract/expected/<cas>.txt`).

`apps/shell/tests/contract.sh` relance chaque commande dans ces conditions et compare sa sortie (stdout, stderr et
code retour) à la référence. Les mêmes entrées doivent toujours donner exactement la même sortie. Le format du contrat (cas,
heures, sorties attendues) est décrit dans [`libs/contract/README.md`](libs/contract/README.md).
Les chemins absolus sont remplacés par `<racine>` pour que les références soient valables sur toutes les machines.

| Fichier | Rôle |
|---|---|
| `libs/contract/cases.txt` | Liste des cas (commande, heure, flux temps réel) |
| `libs/contract/clocks.txt` | Heures figées des cas |
| `apps/shell/tests/contract.sh` | Lance les cas avec le script et compare les sorties |
| `libs/contract/fixtures/gtfs.zip` | Extrait du GTFS : lignes 10, tram et M1, du 10 au 12/10/2026 |
| `libs/contract/fixtures/rt-midi.pb`, `rt-minuit.pb` | Flux temps réel enregistrés le 10/10/2026 à 12:46 et le 11/10 à 00:00 |
| `libs/contract/fixtures/station_*.json` | Quelques stations V'Lille, dont une hors service |
| `libs/contract/fixtures/config/chtilevia/favoris` | Favoris de test |
| `libs/contract/expected/` | Sorties de référence, une par cas |
| `libs/contract/make_fixtures.py` | Régénère les fixtures à partir d'un nouvel instantané (voir plus bas) |

Cas couverts : temps réel avec propagation du retard, passage de minuit, service du lendemain,
flux temps réel indisponible, fin de service du métro, filtres `-d` et `-n` (y compris placés avant
l'arrêt), le mot `vers`, le rapprochement approximatif des arrêts et directions (fautes, phonétique,
ex æquo proposés), `autour` (notations de la position, distances en m et km, erreurs), V'Lille
(accents, plusieurs résultats, station hors service, `--prendre` / `--deposer`), les favoris
(liste, enchaînement, casse du nom, commande en échec, favori inconnu), la sortie parlée `--voix`,
la compréhension des phrases (`vocal` : chiffres en lettres, « à l'arrêt », « en direction de »,
favoris, phrases incomprises), la sortie JSON `--json` (passages, arrêts, V'Lille, favoris,
erreurs avec code, échappement des guillemets) et les messages d'erreur.

#### Lire un échec

Quand une sortie diffère de la référence, le cas est marqué `ÉCHEC` avec la différence :
`-` = ligne attendue absente, `+` = ligne en trop.

```
ÉCHEC bus-options-avant-arret  (chti bus 10 --nombre=2 --vers=chu marais)
      -10 — Eglise Du Marais  (EGLISE DU MARAIS 59160 LOMME)
      -  aucun passage prévu
      -
       10 — Marais  (LOMME MARAIS 59160 LOMME)
```

Deux situations possibles :

- **Régression** (la nouvelle sortie est fausse) : corriger le script, relancer `apps/shell/tests/contract.sh`.
- **Changement voulu** (nouveau format d'affichage, correction de bug…) : régénérer les
  références avec `apps/shell/tests/contract.sh --update`, puis **relire** `git diff libs/contract/expected/` : seules les
  différences attendues doivent apparaître. `--update` enregistre la sortie actuelle telle quelle,
  y compris si elle est fausse : sans relecture, un bug deviendrait la référence.

#### Ajouter un cas

1. Ajouter une ligne dans `libs/contract/cases.txt` (apostrophes autorisées) :
   ```
   nom-du-cas | MIDI | rt-midi.pb | bus 10 gambetta -d flandres
   ```
   - **heure** : `MIDI` (10/10 12:46), `SOIR` (10/10 23:50) ou `MINUIT` (11/10 00:00), voir `libs/contract/clocks.txt` ;
   - **flux temps réel** : `rt-midi.pb`, `rt-minuit.pb`, ou `aucun` pour simuler un flux indisponible ;
   - **arguments** : ceux passés à `chti`. Seules les lignes 10, tram et M1 existent dans les fixtures.
2. Lancer `apps/shell/tests/contract.sh --update`.
3. Relire `libs/contract/expected/nom-du-cas.txt` et vérifier que la sortie est juste.
4. Commiter le cas avec sa référence.

#### Tests Android simulés

`apps/shell/tests/termux.sh` vérifie la partie Termux (`apps/shell/termux/`) sans téléphone. Il crée un faux Termux dans un dossier
temporaire : `pkg`, `termux-speech-to-text`, `termux-notification`, `termux-toast` et `termux-tts-speak` y sont
remplacés par de petites imitations qui notent ce qu'on leur demande dans un journal. Le test lance
ensuite le vrai `apps/shell/termux/install.sh`, puis les vrais boutons du widget sur les fixtures, et vérifie
dans le journal ce qui aurait été affiché et lu à voix haute : *Parler* doit répondre à la voix, les
boutons de favoris par écrit et jamais à la voix. La phrase « entendue » est fixée par
la variable `FAKE_PHRASE`.

#### Régénérer les fixtures

Rarement nécessaire : par exemple si Ilévia change le format de ses données. Les fixtures doivent
alors être recréées à partir d'un nouvel instantané :

```sh
mkdir -p tmp/raw && cd tmp/raw
curl -sL -o gtfs.zip https://media.ilevia.fr/opendata/gtfs.zip && unzip -o gtfs.zip
curl -s -o rt-midi.pb https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt     # en journée
curl -s -o rt-minuit.pb https://proxy.transport.data.gouv.fr/resource/ilevia-lille-gtfs-rt   # vers minuit
curl -s -O https://media.ilevia.fr/opendata/station_information.json
curl -s -O https://media.ilevia.fr/opendata/station_status.json
cd ../..
python3 -I libs/contract/make_fixtures.py tmp/raw libs/contract/fixtures
```

Ensuite :

1. Adapter `DATES` dans `libs/contract/make_fixtures.py` aux jours de l'instantané, puis relancer la commande ci-dessus.
2. Adapter `MIDI`, `SOIR` et `MINUIT` dans `libs/contract/clocks.txt` aux heures de capture (timestamp epoch, ex. `date -j -f '%Y-%m-%d %H:%M:%S' '2026-10-10 12:46:00' +%s`).
3. Lancer `apps/shell/tests/contract.sh --update` et relire **toutes** les références.

Les données brutes restent dans `tmp/` et ne sont pas versionnées.

#### shellcheck

Le shell est piégeux : une variable sans guillemets ou un nom de fichier avec un espace peuvent
faire planter le script dans certains cas seulement. `shellcheck` relit le script sans l'exécuter et
signale ces pièges, comme un correcteur orthographique. S'il n'affiche rien, aucun problème n'a été
trouvé. Un avertissement volontairement ignoré doit être justifié en commentaire
(`# shellcheck disable=SC2086 # raison`).

#### Variables d'environnement

Utilisées par les tests, elles servent aussi à reproduire un cas à la main :

| Variable | Effet |
|---|---|
| `CHTI_NOW` | Heure courante figée (timestamp epoch) |
| `CHTI_GTFS_URL`, `CHTI_RT_URL`, `CHTI_GBFS_URL` | Sources de données (acceptent `file://`) |
| `XDG_CACHE_HOME` | Emplacement du cache (`$XDG_CACHE_HOME/chtilevia`) |
| `XDG_CONFIG_HOME` | Emplacement des favoris (`$XDG_CONFIG_HOME/chtilevia/favoris`) |

```sh
# Rejouer à la main le cas « bus-temps-reel », sans toucher au cache habituel
XDG_CACHE_HOME=/tmp/chti-test CHTI_NOW=1791629160 \
CHTI_GTFS_URL="file://$PWD/libs/contract/fixtures/gtfs.zip" \
CHTI_RT_URL="file://$PWD/libs/contract/fixtures/rt-midi.pb" \
apps/shell/chti bus 10 marais
```

Au premier lancement, le message « Mise à jour du référentiel GTFS… » s'affiche en plus, le temps
de construire le cache de test.

Pour tester le comportement hors ligne : `HTTPS_PROXY=http://127.0.0.1:9 apps/shell/chti bus 10 gambetta`
(le script doit retomber sur les horaires théoriques).

### Commits

Messages en anglais, au format Conventional Commits compact, un commit par changement logique :

```text
feat: add direction filter for transit commands

Users at busy stops only care about one direction; filtering on the
trip headsign keeps the output short.
```
