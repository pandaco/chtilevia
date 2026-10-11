# Serveur MCP chtilevia

Donne aux assistants IA (Claude Code, Claude Desktop, Gemini CLI, tout client
[MCP](https://modelcontextprotocol.io)) les prochains passages Ilévia et la disponibilité V'Lille.
Il tourne en local, en stdio : aucun serveur à héberger.

```
« Quand passe le prochain tram à Croisé Laroche vers Roubaix ? »
  → prochains_passages { mode: "tram", arret: "croisé laroche", direction: "roubaix" }
  → « Dans 1 minute (temps réel), puis dans 13 minutes. »
```

## Outils

| Outil | Paramètres | Réponse (`type`) |
|---|---|---|
| `prochains_passages` | `mode` (bus, tram, metro), `ligne` (sauf tram), `arret`, `direction`?, `nombre`? | `departures` |
| `lister_arrets` | `mode`, `ligne` (sauf tram), `direction`? | `stops` |
| `station_velo` | `station`, `mode`? (tout, prendre, deposer) | `bike_stations` |
| `lister_favoris` | — | `favorites` |
| `favori` | `nom` | `favorite` |

Chaque réponse est un document de [`libs/contract/schema/response.schema.json`](../../libs/contract/schema/response.schema.json),
renvoyé en `structuredContent` (et en texte JSON pour les clients plus anciens). Ce schéma sert
aussi d'`outputSchema` aux outils. Une erreur (`"type": "error"`, code stable, message en
français) est signalée par `isError: true`. Tous les outils sont en lecture seule.

## Installation

Prérequis : Node 24 ou plus récent (il exécute le TypeScript directement, sans compilation), et les
dépendances de `chti` (`curl`, `jq`, `unzip`, `python3`).

```sh
npm install                      # à la racine du dépôt
apps/shell/chti update           # une fois : télécharge les horaires (~1 min), pour que le 1er appel ne fasse pas attendre l'assistant
```

Les favoris sont ceux de `chti` (`~/.config/chtilevia/favoris`).

Dans les commandes ci-dessous, remplacer `/chemin/vers/chtilevia` par le chemin absolu du dépôt.

### Claude Code

```sh
claude mcp add --scope user chtilevia -- node /chemin/vers/chtilevia/apps/mcp/src/main.ts
claude mcp list                  # chtilevia doit apparaître « connected »
```

### Claude Desktop

Dans `~/Library/Application Support/Claude/claude_desktop_config.json` (macOS), puis redémarrer
Claude Desktop. Une application graphique ne voit pas toujours le `PATH` du terminal : indiquer le
chemin complet de `node` (`which node`).

```json
{
  "mcpServers": {
    "chtilevia": {
      "command": "/opt/homebrew/bin/node",
      "args": ["/chemin/vers/chtilevia/apps/mcp/src/main.ts"]
    }
  }
}
```

### Gemini CLI

```sh
gemini mcp add -s user chtilevia node /chemin/vers/chtilevia/apps/mcp/src/main.ts
```

ou, dans `~/.gemini/settings.json` :

```json
{
  "mcpServers": {
    "chtilevia": {
      "command": "node",
      "args": ["/chemin/vers/chtilevia/apps/mcp/src/main.ts"]
    }
  }
}
```

### Autre client / débogage

```sh
npx @modelcontextprotocol/inspector node apps/mcp/src/main.ts
```

Variables d'environnement reconnues : `CHTI_PATH` (autre script `chti`), et toutes celles de `chti`
(`CHTI_NOW`, `CHTI_*_URL`, `XDG_*`, voir le README principal).

## Architecture

```
src/
├── ports/chti-gateway.ts      port : ce dont les outils ont besoin (passages, arrêts, vélos, favoris)
├── adapters/shell-gateway.ts  adaptateur : lance `chti --json` (v1)
├── server.ts                  adaptateur d'entrée MCP : outils, validation des paramètres
├── contract-schema.ts         schémas de sortie tirés de libs/contract
└── main.ts                    racine de composition, transport stdio
```

Le serveur ne connaît que le port `ChtiGateway`. La v2 remplacera `ShellGateway` par le cœur
Kotlin compilé pour Node, sans toucher aux outils ni aux tests.

Garde-fous : les textes reçus ne peuvent pas commencer par `-` (pas d'option de `chti` injectée),
arrêt et direction sont passés d'un seul tenant, et `favori` n'accepte qu'un nom présent dans la
liste des favoris (`favori update` ne relance pas le téléchargement des horaires).

## Tests

```sh
npx nx run-many -t lint test -p mcp      # typage (tsc) + tests
```

`test/contract.test.ts` relie un client MCP au serveur en mémoire et vérifie que chaque appel
d'outil rend **exactement** le document d'un cas `json-*` de `libs/contract` (mêmes fixtures, même
heure figée). Il valide aussi toutes les sorties JSON du contrat avec le schéma (ajv), et les
garde-fous ci-dessus.
