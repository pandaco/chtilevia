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
chmod +x chti
ln -s "$PWD/chti" ~/.local/bin/chti   # optionnel, pour l'avoir dans le PATH
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
| `chti <favori>` | Lance la ou les commandes enregistrées sous ce nom (voir [Favoris](#favoris)) |
| `chti favoris` | Liste les favoris |
| `chti --voix <commande>` | Même réponse, en phrases à lire à voix haute (voir [Sortie parlée](#sortie-parlée)) |
| `chti update` | Force le re-téléchargement du référentiel GTFS |
| `chti help` | Affiche l'aide |

### Paramètres

- **`<ligne>`** : nom de la ligne tel qu'affiché sur le réseau — `10`, `L1`, `C5`, `CO3`, `Z2`… (insensible à la casse).
- **`<arrêt>`** / **`<station>`** : tout ou partie du nom, sur un ou plusieurs mots, sans guillemets nécessaires.
  Casse, accents et ponctuation sont ignorés. Si plusieurs arrêts correspondent (ex. « gambetta »
  existe dans plusieurs communes), chacun est affiché avec sa commune.
- **`-d <direction>`** (alias `--direction`, `--vers`, ou `--vers=<direction>`) : ne garde que les
  courses dont la destination contient ce texte. Placée n'importe où après la ligne.
  Une direction inconnue affiche la liste des directions possibles.
  Le mot **`vers`** sans tiret marche aussi : tous les mots qui le suivent forment la direction
  (`chti bus 10 gambetta vers lille flandres`).
- **`-n <nombre>`** (alias `--nombre`, ou `--nombre=<nombre>`) : nombre de prochains passages affichés
  par direction, entier ≥ 1, **3 par défaut**. Si le service du jour ne suffit pas, les premiers
  départs du lendemain sont affichés. Combinable avec `-d`, dans n'importe quel ordre.
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

- Un fichier d'exemple est fourni : `mkdir -p ~/.config/chtilevia && cp exemples/favoris ~/.config/chtilevia/`.
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
pensées pour la synthèse vocale. Il est pensé pour un usage vocal, mais
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

## Contribution

### Organisation du script

Tout tient dans le fichier `chti` :

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

Deux outils vérifient le script. Ils ne servent pas à l'usage quotidien, seulement **après
chaque modification**, pour s'assurer qu'on n'a rien cassé.

```sh
tests/run.sh                      # tests de non-régression : 21 cas, hors ligne, ~5 s
shellcheck chti tests/run.sh    # analyse du shell (paquet shellcheck)
```

**Avant chaque commit**, les deux doivent passer : `tests/run.sh` doit afficher `0 échec(s)`, et
`shellcheck` ne doit rien afficher.

#### Pourquoi des tests « figés » ?

Les horaires changent chaque minute : en lançant `chti bus 10 marais` à la main, impossible de
savoir si « 12:53 dans 7 min » est juste ou faux, puisque la bonne réponse change sans arrêt.
Les tests figent donc le monde :

1. **Données figées** : une copie des flux enregistrée à un instant précis (`tests/fixtures/`).
2. **Heure figée** : le script croit qu'on est à cet instant-là (variable `CHTI_NOW`).
3. **Réponses de référence** : la sortie correcte de chaque commande, relue et enregistrée une fois
   pour toutes (`tests/expected/<cas>.txt`).

`tests/run.sh` relance chaque commande dans ces conditions et compare sa sortie (stdout, stderr et
code retour) à la référence. Les mêmes entrées doivent toujours donner exactement la même sortie.
Les chemins absolus sont remplacés par `<racine>` pour que les références soient valables sur toutes les machines.

| Fichier | Rôle |
|---|---|
| `tests/run.sh` | Liste des cas et comparaison des sorties |
| `tests/fixtures/gtfs.zip` | Extrait du GTFS : lignes 10, tram et M1, du 10 au 12/10/2026 |
| `tests/fixtures/rt-midi.pb`, `rt-minuit.pb` | Flux temps réel enregistrés le 10/10/2026 à 12:46 et le 11/10 à 00:00 |
| `tests/fixtures/station_*.json` | Quelques stations V'Lille, dont une hors service |
| `tests/fixtures/config/chtilevia/favoris` | Favoris de test |
| `tests/expected/` | Sorties de référence, une par cas |
| `tests/make_fixtures.py` | Régénère les fixtures à partir d'un nouvel instantané (voir plus bas) |

Cas couverts : temps réel avec propagation du retard, passage de minuit, service du lendemain,
flux temps réel indisponible, fin de service du métro, filtres `-d` et `-n` (y compris placés avant
l'arrêt), le mot `vers`, V'Lille (accents, plusieurs résultats, station hors service, `--prendre` / `--deposer`), les favoris
(liste, enchaînement, casse du nom, commande en échec, favori inconnu), la sortie parlée `--voix`
et les messages d'erreur.

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

- **Régression** (la nouvelle sortie est fausse) : corriger le script, relancer `tests/run.sh`.
- **Changement voulu** (nouveau format d'affichage, correction de bug…) : régénérer les
  références avec `tests/run.sh --update`, puis **relire** `git diff tests/expected/` : seules les
  différences attendues doivent apparaître. `--update` enregistre la sortie actuelle telle quelle,
  y compris si elle est fausse : sans relecture, un bug deviendrait la référence.

#### Ajouter un cas

1. Ajouter une ligne dans la variable `CASES` de `tests/run.sh` :
   ```
   nom-du-cas | MIDI | rt-midi.pb | bus 10 gambetta -d flandres
   ```
   - **heure** : `MIDI` (10/10 12:46), `SOIR` (10/10 23:50) ou `MINUIT` (11/10 00:00) ;
   - **flux temps réel** : `rt-midi.pb`, `rt-minuit.pb`, ou `aucun` pour simuler un flux indisponible ;
   - **arguments** : ceux passés à `chti`. Seules les lignes 10, tram et M1 existent dans les fixtures.
2. Lancer `tests/run.sh --update`.
3. Relire `tests/expected/nom-du-cas.txt` et vérifier que la sortie est juste.
4. Commiter le cas avec sa référence.

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
python3 -I tests/make_fixtures.py tmp/raw tests/fixtures
```

Ensuite :

1. Adapter `DATES` dans `tests/make_fixtures.py` aux jours de l'instantané, puis relancer la commande ci-dessus.
2. Adapter `MIDI`, `SOIR` et `MINUIT` dans `tests/run.sh` aux heures de capture (timestamp epoch, ex. `date -j -f '%Y-%m-%d %H:%M:%S' '2026-10-10 12:46:00' +%s`).
3. Lancer `tests/run.sh --update` et relire **toutes** les références.

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
CHTI_GTFS_URL="file://$PWD/tests/fixtures/gtfs.zip" \
CHTI_RT_URL="file://$PWD/tests/fixtures/rt-midi.pb" \
./chti bus 10 marais
```

Au premier lancement, le message « Mise à jour du référentiel GTFS… » s'affiche en plus, le temps
de construire le cache de test.

Pour tester le comportement hors ligne : `HTTPS_PROXY=http://127.0.0.1:9 ./chti bus 10 gambetta`
(le script doit retomber sur les horaires théoriques).

### Commits

Messages en anglais, au format Conventional Commits compact, un commit par changement logique :

```text
feat: add direction filter for transit commands

Users at busy stops only care about one direction; filtering on the
trip headsign keeps the output short.
```
