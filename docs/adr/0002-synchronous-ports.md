# Des ports synchrones sur toutes les cibles

Les ports du cœur (`TimetableRepository`, `TimetableUpdater`, `RealtimeFeed`, `BikeShareFeed`, `FavoritesStore`) sont de simples fonctions bloquantes, sans `suspend`, identiques pour la JVM, Android et Node. Les cas d'usage, le domaine, le décodage (CSV, protobuf, JSON, zip) et le cache GTFS s'écrivent ainsi une seule fois, en Kotlin commun ; seul l'accès au système change d'une plateforme à l'autre.

Node n'ayant pas d'appel HTTP synchrone, le téléchargement y passe par un processus `node` enfant attendu (`execFileSync`), qui fait un `fetch` limité à 30 s : le contenu sort sur la sortie standard, un code retour non nul signale l'échec. Fichiers et décompression utilisent les variantes `*Sync` de `node:fs` et `node:zlib`.

## Options écartées

- **Ports `suspend` partout** : les coroutines auraient traversé toutes les couches, jusqu'au domaine et aux exécuteurs du contrat, pour le seul bénéfice de Node.
- **Réseau délégué à `curl`** : le serveur MCP doit tourner sans outil externe (ni curl, ni jq, ni python3, contrairement au shell).

## Conséquences

- La concurrence revient aux appelants : Android exécute le cœur hors du thread principal (exécuteur `chti`, `Dispatchers.IO`).
- Sous Node, une commande bloque la boucle d'événements pendant toute sa durée : le serveur MCP traite une recherche à la fois. C'est acceptable pour un serveur stdio à client unique ; un serveur HTTP multi-clients ferait plutôt tourner le cœur dans un `worker_thread` que de rendre les ports asynchrones.
- Le coût mesuré (octobre 2026, Node 26) ne vient pas du caractère synchrone. Un `node` enfant coûte environ 80 ms par téléchargement, soit une partie seulement d'une recherche de 300 à 450 ms ; une recherche de métro, sans aucun réseau, prend déjà 300 ms, car le cœur Node reconstruit tout et relit ses fichiers à chaque appel. Le rafraîchissement quotidien du référentiel (environ 6 s) bloque la recherche qui le déclenche.
- Les gains, s'ils deviennent nécessaires, sont ailleurs : garder le référentiel en mémoire entre deux appels sous Node, comme Android le fait déjà, et rafraîchir le référentiel au démarrage du serveur MCP plutôt que pendant une recherche.
