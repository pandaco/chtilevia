# 5. Le cœur Kotlin

> **Dans ce chapitre :** les six modules de `libs/`, ce que chacun a le droit d'importer, et comment
> construire et tester le cœur pour la JVM, Android et Node.

La logique de Chtipasse est portée en Kotlin Multiplatform, en architecture hexagonale (voir
[L'architecture](02-architecture.md)). Le cœur se comporte exactement comme le script shell : il
passe les mêmes cas du contrat, au caractère près. Chaque module compile pour la JVM, Android et
Node.js.

## Les modules

| Module | Rôle | Dépend de |
|---|---|---|
| `libs/domain` | Modèles et règles métier purs : recherche sans accents et approximative (`ApproximateMatch`), jours de service, fusion théorique / temps réel, distances | rien (sauf kotlinx-datetime) |
| `libs/application` | Cas d'usage (`GetDepartures`, `GetBikeStations`, `FindNearby`, `SuggestQuery`, `UpdateTimetable`, favoris), ports, grammaire des commandes (`CommandLine`), phrase libre (`InterpretUtterance`), profil du réseau | domain |
| `libs/presentation` | `Chti`, l'équivalent de la commande ; sorties texte, voix, JSON du contrat et compacte (`--court`) | application |
| `libs/adapters` | GTFS statique (cache partagé avec le shell), GTFS-RT, V'Lille (GBFS), fichier des favoris, zip | application |
| `libs/node` | Racine de composition pour Node : `runChti`, la commande exportée en module ES pour le serveur MCP | presentation, adapters |
| `libs/core-tests` | Règles d'architecture (Konsist) et contrat joué par le cœur | tout |

### Les ports

Les cas d'usage ne connaissent le monde extérieur que par des interfaces
(`libs/application/.../Ports.kt`) :

| Port | Implémenté par |
|---|---|
| `TimetableRepository` | `GtfsTimetable` (fichiers de l'index), `CachedTimetable` (gardé en mémoire, pour l'appli) |
| `TimetableUpdater` | `GtfsCache` (téléchargement, index, rafraîchissement toutes les 24 h) |
| `RealtimeFeed` | `GtfsRealtimeFeed` |
| `BikeShareFeed` | `GbfsFeed` |
| `FavoritesStore` | `FileFavoritesStore` (même format `nom: commande` que le shell, commentaires conservés) |
| `Clock` | une lambda dans chaque racine de composition : horloge système, ou heure figée (`CHTI_NOW`) |

Pour brancher une nouvelle source de données, on écrit un adaptateur pour le port concerné : aucun
cas d'usage ne change.

## Les garde-fous d'architecture

`libs/core-tests/.../ArchitectureTest.kt` fait échouer le build si :

- le domaine dépend d'autre chose que la bibliothèque standard (et kotlinx-datetime) ;
- l'application dépend d'autre chose que du domaine ;
- la présentation dépend d'autre chose que du domaine et de l'application ;
- les adaptateurs dépendent de la présentation ou des applis ;
- un fichier du cœur n'est pas dans le paquet de sa couche.

`ContractTest`, dans le même module, crée un test par cas de `libs/contract/cases.txt` : `Chti` est
lancé sur les fixtures, à l'heure du cas, et sa sortie doit être identique à `expected/<cas>.txt`.

## Construire et tester

```sh
./gradlew test                       # tests de tous les modules, sur toutes les cibles
./gradlew :domain:jvmTest            # une seule cible
./gradlew :core-tests:test           # architecture et contrat
npx nx run :node:build               # le module ES pour Node (utilisé par le serveur MCP)
```

- La configuration commune des modules est dans `buildSrc/` (plugin `chtipasse.kmp-library`) et
  les versions dans `gradle/libs.versions.toml`.
- Nx lance Gradle mais **ne met pas en cache ses tâches** : les entrées qu'il en déduit ne suivent
  pas les dépendances (une sortie d'avant un changement de version pouvait être restaurée). Gradle a
  son propre cache et sait ce qui est à jour.

## Ce qui est propre à chaque plateforme

Les adaptateurs sont communs à toutes les cibles (décodage CSV, protobuf, JSON, zip ; cache GTFS).
Seul l'accès au système est propre à chaque plateforme (`Platform.kt` et ses `actual`) :

- `java.io` / `java.net` sur la JVM et Android (source set `jvmShared`) ;
- modules `node:*` sous Node. Le téléchargement passe par un processus `node` enfant, pour garder
  des ports synchrones.

---

**Et ensuite →** [6. L'appli Android](06-appli-android.md) : comment le cœur devient un écran, un
widget et une carte.
