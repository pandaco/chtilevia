# Documentation de Chtipasse

## Utiliser Chtipasse

| Guide | Pour |
|---|---|
| [Ce que Chtipasse comprend](guide/questions.md) | Savoir quoi demander, et comment Chtipasse rattrape les fautes |
| [L'appli Android](guide/appli-android.md) | L'écran, le widget, les favoris, la carte, les autorisations |
| [La commande `chti`](guide/terminal.md) | Toutes les commandes du terminal, les favoris, les sorties voix et JSON |
| [Termux](guide/termux.md) | Installer `chti` et son widget dans Termux sur Android |
| [Assistants IA](guide/assistants-ia.md) | Brancher Chtipasse à Claude, Antigravity ou aux agents du téléphone |
| [Confidentialité](confidentialite.md) | Ce que Chtipasse fait (et ne fait pas) de tes données |

## Comprendre et contribuer

Un parcours en neuf chapitres courts, à lire dans l'ordre : chacun s'appuie sur le précédent et se
termine par un lien vers le suivant.

| | Chapitre | Tu y apprends |
|---|---|---|
| 1 | [Premiers pas](dev/01-premiers-pas.md) | Lancer `chti`, construire l'appli, faire passer tous les tests |
| 2 | [L'architecture](dev/02-architecture.md) | Trois implémentations, un contrat ; le trajet d'une question |
| 3 | [Les données](dev/03-donnees.md) | GTFS, GTFS-RT, GBFS, le cache et les limites |
| 4 | [Le contrat et les tests](dev/04-contrat-et-tests.md) | Tester ce qui change chaque minute ; lire un échec ; ajouter un cas |
| 5 | [Le cœur Kotlin](dev/05-coeur-kotlin.md) | Les modules, les ports, les règles d'architecture |
| 6 | [L'appli Android](dev/06-appli-android.md) | Écrans, widget, service, AppFunctions, mesures |
| 7 | [Le script shell](dev/07-script-shell.md) | L'implémentation de référence, en un seul fichier |
| 8 | [Le serveur MCP](dev/08-serveur-mcp.md) | Les outils pour assistants IA et leurs garde-fous |
| 9 | [Contribuer](dev/09-contribuer.md) | Conventions, commits, une fonctionnalité de bout en bout |

## Références

- [`libs/contract/README.md`](../libs/contract/README.md) : le format exact des cas et des sorties
  attendues.
- [`apps/mcp/README.md`](../apps/mcp/README.md) : les outils MCP, leurs paramètres et l'architecture
  du serveur.
- [`fastlane/metadata/android/fr-FR/`](../fastlane/metadata/android/fr-FR/) : la fiche Play Store.
