# Chtipasse dans le terminal : la commande `chti`

Les mêmes réponses que l'appli, sans quitter le terminal. `chti` est un script shell unique : il
fonctionne sur macOS, sous Linux et dans [Termux](termux.md) sur Android.

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

## Installation

Dépendances : `curl`, `jq`, `unzip` et `python3` (bibliothèque standard uniquement).

Dans le dossier du dépôt :

```sh
chmod +x apps/shell/chti
ln -s "$PWD/apps/shell/chti" ~/.local/bin/chti   # optionnel, pour l'avoir dans le PATH
chti help
```

Au premier appel, `chti` télécharge les horaires (environ 9 Mo) et les prépare. Ensuite, ils sont
rafraîchis automatiquement toutes les 24 h.

## Toutes les commandes

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
| `chti autour [mode] <latitude> <longitude> [-n <nombre>]` | Arrêts et stations V'Lille les plus proches, à vol d'oiseau (voir [À proximité](#à-proximité)) |
| `chti <favori>` | Lance la ou les commandes enregistrées sous ce nom (voir [Favoris](#favoris)) |
| `chti favoris` | Liste les favoris |
| `chti --voix <commande>` | Même réponse, en phrases à lire à voix haute (voir [Sortie parlée](#sortie-parlée)) |
| `chti --json <commande>` | Même réponse, en JSON pour les programmes (voir [Sortie JSON](#sortie-json)) |
| `chti vocal "<phrase>"` | Comprend une phrase libre et répond en mode voix (voir [Phrases](#phrases-libres)) |
| `chti update` | Force le re-téléchargement du référentiel GTFS |
| `chti help` | Affiche l'aide |

### Paramètres

- **`<ligne>`** : le nom de la ligne tel qu'affiché sur le réseau, par exemple `10`, `L1`, `C5`,
  `CO3` ou `Z2` (la casse est ignorée).
- **`<arrêt>`** / **`<station>`** : tout ou partie du nom, en un ou plusieurs mots, sans guillemets.
  Casse, accents et ponctuation sont ignorés. Si plusieurs arrêts correspondent (« gambetta » existe
  dans plusieurs communes), chacun est affiché avec sa commune.
  Si aucun arrêt ne contient le texte, chti prend celui qui lui ressemble le plus, à l'écrit ou au
  son : fautes de frappe ou de reconnaissance vocale (`gambeta`, `croix la roche`, `saint mort`),
  mots collés ou séparés, mot manquant (`eglise marais`), nombres en lettres (`quatre cantons` pour
  « 4 Cantons »). Si plusieurs arrêts ressemblent autant à la demande, chti n'en choisit aucun et
  les propose (code `ambiguous_stop`). Les stations V'Lille suivent la même règle, mais toutes
  celles qui ressemblent autant à la demande sont affichées (`rihourr`, `sainte élaine`).
- **`-d <direction>`** (alias `--direction`, `--vers`, ou `--vers=<direction>`) : ne garde que les
  courses dont la destination contient ce texte (à défaut, celles dont la destination lui ressemble
  le plus). L'option peut se placer n'importe où après la ligne.
  Une direction inconnue affiche la liste des directions possibles. Un arrêt qui existe mais n'est
  pas desservi dans cette direction le dit, avec les directions qui le desservent.
  Le mot **`vers`** sans tiret marche aussi : tous les mots qui le suivent forment la direction
  (`chti bus 10 gambetta vers lille flandres`).
- **`-n <nombre>`** (alias `--nombre`, ou `--nombre=<nombre>`) : le nombre de prochains passages
  affichés par direction, un entier ≥ 1, **3 par défaut**. Si le service du jour ne suffit pas, les
  premiers départs du lendemain sont affichés. Se combine avec `-d`, dans n'importe quel ordre.
- **`--prendre`** / **`--deposer`** (ou `--déposer`) : pour V'Lille, n'affiche que les vélos
  disponibles, ou que les places libres. Indique aussi « location / dépôt impossible » si la station
  le refuse.
- **`metro`** accepte `1`, `2`, `m1` et `m2`. `métro`, `vélo` et `vlille` sont acceptés comme alias.

### Exemples

```sh
chti bus 10                              # arrêts de la ligne 10
chti bus 10 gambetta                     # passages à « Gambetta », dans tous les sens
chti bus l1 republique -d wambrechies    # passages de la Liane 1 vers Wambrechies
chti tram croise laroche --vers roubaix
chti tram croise laroche vers roubaix    # « vers » sans tiret
chti tram -d tourcoing                   # arrêts du tram vers Tourcoing
chti metro 1 republique -d chu
chti metro 2 gare lille flandres -n 10   # les 10 prochaines rames dans chaque sens
chti bus 10 gambetta -d flandres -n 5    # les 5 prochains bus vers Lille Flandres
chti velo gare                           # toutes les stations V'Lille contenant « gare »
chti autour velo 50.6366 3.0630          # les stations V'Lille les plus proches
chti travail                             # un favori
```

### Lire le résultat

- Les N prochains passages par direction (`-n`, 3 par défaut). Au-delà d'une heure, l'attente
  s'affiche en heures (`dans 8 h 04`).
- `⏱  temps réel` : horaire issu du suivi des véhicules ; `🕒 théorique` : horaire planifié.
- Le code retour vaut `0` en cas de succès, et `1` si la ligne, l'arrêt, la direction, la station
  ou le favori est introuvable, ou si le service est injoignable.

## À proximité

`chti autour <latitude> <longitude>` donne les arrêts les plus proches (avec leurs lignes) et les
stations V'Lille les plus proches (avec vélos et places), triés par distance à vol d'oiseau.

- **Position** : en degrés décimaux, comme les donne une appli de carte : `50.6366 3.0630`,
  `50.6366, 3.0630`, `50.6366,3.0630` ou `50,6366 3,0630`.
- **Mode** (facultatif, juste après `autour`) : `bus`, `tram`, `metro` ou `velo`, pour ne garder
  que ces arrêts ou que les stations V'Lille.
- **`-n`** : le nombre d'arrêts et de stations donnés (3 par défaut).

## Favoris

Un favori donne un nom court à une ou plusieurs commandes : `chti travail` au lieu de
`chti tram croise laroche vers roubaix -n 2`. Ils sont lus dans `~/.config/chtipasse/favoris`
(ou `$XDG_CONFIG_HOME/chtipasse/favoris`), au format `nom: commande` :

```
# Le tram pour aller au travail
travail:      tram croise laroche vers roubaix -n 2

# Rentrer à vélo : vélos au départ, places à l'arrivée (un nom répété = commandes enchaînées)
rentrer-velo: velo gare lille europe --prendre
rentrer-velo: velo la madeleine gare --deposer
```

- Un fichier d'exemple est fourni : `mkdir -p ~/.config/chtipasse && cp apps/shell/exemples/favoris ~/.config/chtipasse/`.
- La commande est celle que tu taperais après `chti`. `#` commence un commentaire.
- Le nom ignore la casse et peut compter plusieurs mots ; il ne doit pas reprendre une commande
  existante (`bus`, `velo`…).
- Si une des commandes échoue, les suivantes sont quand même lancées et le code retour vaut `1`.
- Attention aux directions trop larges : pour le bus 10, « vers lille » correspond aux deux sens
  (Lille Flandres et Lille CHU). Vérifie avec `chti bus 10 -d lille`.

C'est le même fichier, au même format, que celui de l'appli Android et du serveur MCP.

## Sortie parlée

`--voix` (placé n'importe où) remplace l'affichage par des phrases simples, sans emoji ni colonnes,
pensées pour la synthèse vocale :

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

## Sortie JSON

`--json` (placé n'importe où) remplace l'affichage par un document JSON unique sur stdout, pour les
programmes : serveur MCP, scripts, widgets. Sa forme est décrite par le schéma
[`libs/contract/schema/response.schema.json`](../../libs/contract/schema/response.schema.json).

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
  `nearby`, `favorites`, `favorite` (un résultat par commande du favori), `update` ou `error`.
- Les erreurs sont aussi du JSON sur stdout (`"type": "error"`), avec un `code` stable
  (`unknown_line`, `unknown_direction`…), le `message` en français et parfois des `details`
  (directions possibles). Le code retour reste `1`.
- Les avertissements (flux temps réel indisponible, GTFS non mis à jour) sont dans `warnings`, pas
  sur stderr.
- Pour V'Lille, le JSON donne les faits bruts (vélos, places, location et dépôt possibles) ;
  `--prendre` et `--deposer` sont seulement rappelés dans `mode`.
- Incompatible avec `--voix` et avec `vocal` : un programme interprète la phrase lui-même.

## Phrases libres

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
| « Vélo à proximité », « les arrêts autour de moi » | `autour`, complété de la position par l'appli Android |

Les règles sont détaillées dans [Ce que Chtipasse comprend](questions.md). Le shell ne connaît pas
la position du téléphone : une phrase « à proximité » y demande donc une position, que l'appli
Android, elle, ajoute toute seule (avec ton accord).

---

Suite : [Termux sur Android](termux.md) · [Assistants IA](assistants-ia.md) ·
[Comment ça marche ?](../dev/01-premiers-pas.md)
