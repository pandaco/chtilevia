# Chtipasse avec un assistant IA

Pose ta question à ton assistant comme à un collègue : il interroge Chtipasse et te répond avec les
vrais horaires du réseau, pas avec une estimation.

```
Toi    : Je pars de Croisé Laroche, j'ai le temps de prendre un café avant le tram pour Roubaix ?
Claude : → prochains_passages { mode: "tram", arret: "croisé laroche", direction: "roubaix" }
         Le prochain tram passe dans 1 minute (temps réel), le suivant dans 13 minutes :
         tu as le temps pour un café rapide si tu vises le second.
```

Chtipasse se branche de deux façons :

- **sur l'ordinateur**, par un serveur [MCP](https://modelcontextprotocol.io) local, pour Claude Code,
  Claude Desktop, Antigravity ou tout client MCP ;
- **sur le téléphone**, par **AppFunctions**, pour les agents d'Android 16 et plus (Gemini…).

Dans les deux cas, l'assistant a accès aux mêmes outils, tous en **lecture seule** :

| Outil (MCP) | Fonction (AppFunctions) | Ce qu'il donne |
|---|---|---|
| `prochains_passages` | `prochainsPassages` | Prochains passages d'une ligne à un arrêt, éventuellement vers une direction |
| `lister_arrets` | `listerArrets` | Arrêts d'une ligne, dans chaque sens |
| `station_velo` | `stationVelo` | Vélos et places libres d'une station V'Lille |
| `autour_de_moi` | `autourDeMoi` | Arrêts et stations les plus proches d'une position, filtrables par mode |
| `lister_favoris` | `listerFavoris` | Tes favoris |
| `favori` | `favori` | La réponse d'un favori |

## Sur l'ordinateur : le serveur MCP

Le serveur tourne en local, en stdio : il n'y a aucun serveur à héberger et aucune clé d'API à
fournir. Il s'appuie sur le cœur Kotlin compilé pour Node : ses réponses sont exactement celles de
`chti --json`.

**Prérequis** : Node 24 ou plus récent, et de quoi compiler le cœur Kotlin (JDK 17+, SDK Android :
voir [Premiers pas](../dev/01-premiers-pas.md)).

```sh
npm install                      # à la racine du dépôt
npx nx run :node:build           # compile le cœur Kotlin pour Node (à refaire après une mise à jour)
```

Dans les commandes ci-dessous, remplace `/chemin/vers/chtipasse` par le chemin absolu du dépôt.

### Claude Code

```sh
claude mcp add --scope user chtipasse -- node /chemin/vers/chtipasse/apps/mcp/src/main.ts
claude mcp list                  # chtipasse doit apparaître « connected »
```

### Claude Desktop

Dans `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS), puis redémarre
Claude Desktop. Une application graphique ne voit pas toujours le `PATH` du terminal : indique le
chemin complet de `node` (`which node`).

```json
{
  "mcpServers": {
    "chtipasse": {
      "command": "/opt/homebrew/bin/node",
      "args": ["/chemin/vers/chtipasse/apps/mcp/src/main.ts"]
    }
  }
}
```

### Antigravity

```sh
agy mcp add chtipasse node /chemin/vers/chtipasse/apps/mcp/src/main.ts
agy mcp list                     # chtipasse doit apparaître « enabled »
```

ou, dans `~/.gemini/config/mcp_config.json` :

```json
{
  "mcpServers": {
    "chtipasse": {
      "command": "node",
      "args": ["/chemin/vers/chtipasse/apps/mcp/src/main.ts"]
    }
  }
}
```

### Bon à savoir

- Le premier appel télécharge et indexe les horaires (quelques secondes). Le cache
  (`~/.cache/chtipasse`) est partagé avec la commande `chti`.
- Les favoris sont ceux de `chti` (`~/.config/chtipasse/favoris`).
- Sans JDK, le serveur peut s'appuyer sur le script shell : lance-le avec `CHTI_GATEWAY=shell`
  (il faut alors `curl`, `jq`, `unzip` et `python3`).

## Sur le téléphone : AppFunctions

L'[appli Android](appli-android.md) expose les mêmes outils aux agents du téléphone par
[AppFunctions](https://developer.android.com/ai/appfunctions) (Android 16 et plus). Chaque fonction
répond en phrases à lire (`text`) et en JSON (`json`) ; une erreur (arrêt introuvable, argument
invalide…) remonte à l'agent avec le message de Chtipasse.

Gemini n'appelle ces fonctions que pour les testeurs de son programme d'accès anticipé. Sans y être
inscrit, tu peux les essayer avec `adb` :

```sh
adb shell cmd app_function list-app-functions --package fr.chtipasse
adb shell "cmd app_function execute-app-function --package fr.chtipasse \
  --function 'fr.chtipasse.android.BaseChtiAppFunctionService#prochainsPassages' \
  --parameters '{\"mode\":\"tram\",\"arret\":\"croisé laroche\",\"direction\":\"roubaix\"}' --brief-yaml"
```

D'ici là, le widget *Parler* reste la voie vocale sur le téléphone.

---

Pour aller plus loin : [le serveur MCP côté code](../../apps/mcp/README.md) ·
[Ce que Chtipasse comprend](questions.md)
