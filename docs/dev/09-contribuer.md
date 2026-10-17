# 9. Contribuer

> **Dans ce chapitre :** les conventions du projet, et la recette complète pour ajouter une
> fonctionnalité, du premier cas de test jusqu'au bouton dans l'appli.

## Conventions

- **Langue** : commentaires et docstrings en français ; identifiants (variables, fonctions, classes,
  fichiers) en anglais ; messages pour l'utilisateur en français.
- **Zéro dépendance pour le shell** : sh POSIX, `curl`, `jq`, `unzip`, Python bibliothèque standard
  uniquement.
- **Erreurs** : sur stderr dans le shell (via `die`), avec un code stable en JSON ; code retour non
  nul en cas d'échec.
- **Versions** : exactes, publiées depuis au moins deux semaines au moment du choix
  (`gradle/libs.versions.toml`, `package.json`).
- **Dépendances verrouillées** : chaque module Gradle a son `gradle.lockfile`. Après un ajout ou une
  montée de version, régénère-les avec `./gradlew resolveAndLockAll --write-locks` et commite-les
  avec le changement.
- **Zéro avertissement** : un avertissement du compilateur Kotlin, d'un script `*.gradle.kts` ou du
  lint Android fait échouer le build. On le corrige, on ne le masque pas.
- **Sécurité** : `npx nx run chtipasse:security` lance [Trivy](https://trivy.dev) (dépendances npm
  et Gradle, secrets, configuration ; voir `trivy.yaml`) et échoue dès une faille HIGH ou CRITICAL
  corrigeable. Il faut l'installer une fois (`brew install trivy`).
- **Fichiers de travail** : dans `./tmp`, ignoré par git. Rien de personnel dans les exemples : les
  trajets d'exemple restent neutres (tram Croisé Laroche, V'Lille Gare Lille Europe → La Madeleine
  Gare).

## Recette : une nouvelle fonctionnalité de bout en bout

Exemple : ajouter une option, une nouvelle question ou une nouvelle information à une réponse.

1. **Écris les cas d'abord.** Ajoute les lignes dans `libs/contract/cases.txt` (texte, `--voix`,
   `--json`, erreurs). Ils fixent le comportement attendu de toutes les implémentations. Voir
   [Ajouter un cas](04-contrat-et-tests.md#ajouter-un-cas).
2. **Le shell** (`apps/shell/chti`) : implémente, puis génère les références avec
   `apps/shell/tests/contract.sh --update` et **relis-les** une à une.
3. **Le schéma JSON** : si la réponse `--json` change de forme, mets à jour
   `libs/contract/schema/response.schema.json`. Le serveur MCP le valide.
4. **Le cœur Kotlin** : la règle va dans `libs/domain`, le cas d'usage ou la grammaire dans
   `libs/application`, le format dans `libs/presentation`. Lance `./gradlew :core-tests:test` :
   le contrat et l'architecture doivent passer.
5. **Les façades** : si c'est un nouvel outil, ajoute-le dans `apps/mcp/src/server.ts` **et** dans
   `ChtiAppFunctions.kt`. Le test de parité y veille.
6. **L'appli** : l'aide (`Help.kt`), la saisie assistée (`SuggestQuery`) et, si besoin, les cartes de
   réponse.
7. **La documentation** : le [guide utilisateur](../guide/questions.md), la référence du
   [terminal](../guide/terminal.md) et, si la fonction est visible, le README.
8. **Tout vérifier** : `npx nx run-many -t lint test build` puis `npx nx run chtipasse:security`
   doivent être verts. La CI (`.github/workflows/ci.yml`) rejoue les deux à chaque push sur `main`.

## Commits

Messages en français, au format Conventional Commits compact, un commit par changement logique.
Le type reste le mot-clé standard (`feat`, `fix`, `perf`, `docs`, `test`, `refactor`, `build`,
`ci`, `chore`) : les versions et le changelog en sont déduits.

```text
feat: ajouter le filtre de direction aux commandes de transport

Aux arrêts chargés, on ne s'intéresse qu'à une direction ; filtrer sur
la direction des courses garde une sortie courte.
```

Le corps explique **pourquoi** plutôt que quoi. Une fonctionnalité qui touche plusieurs couches peut
se découper en commits successifs (cas du contrat et shell, puis cœur, puis façades), chacun laissant
les tests au vert.

## Licence

En contribuant, tu acceptes que ta contribution soit publiée sous la licence du projet,
[GPL-3.0-or-later](../../LICENSE).

---

**Tu as fait le tour !** Retour au [sommaire de la documentation](../README.md).
