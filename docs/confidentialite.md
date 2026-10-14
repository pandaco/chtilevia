# Politique de confidentialité

*Dernière mise à jour : 14 octobre 2026. Elle s'applique à l'appli Android Chtipasse
(`fr.chtipasse`), à la commande `chti` et au serveur MCP Chtipasse.*

**En bref : Chtipasse ne collecte rien.** Il n'y a ni compte, ni publicité, ni outil de mesure
d'audience, ni rapport de plantage envoyé, ni serveur Chtipasse. Tout se calcule sur ton appareil.

## Ce qui reste sur ton appareil

| Donnée | Où | Pourquoi |
|---|---|---|
| Tes favoris (noms et questions) | Fichier `favoris` dans l'espace privé de l'appli (`~/.config/chtipasse/` pour `chti`) | Lancer tes trajets en un geste |
| Tes réglages (voix activée, thème) | Préférences privées de l'appli | Les retrouver au prochain lancement |
| Les horaires du réseau | Espace privé de l'appli (`~/.cache/chtipasse/` pour `chti`) | Répondre vite, même sans réseau |

Chtipasse ne les envoie nulle part. Seule exception : si la sauvegarde Android est activée sur ton
téléphone, Android peut les inclure dans ta sauvegarde personnelle (dans ton compte Google), comme
pour toute appli. Désinstaller l'appli (ou effacer ses données) les supprime du téléphone.

## Ce que l'appli télécharge

Chtipasse ne fait que **télécharger** des données publiques. Il n'envoie ni ta question, ni tes
favoris, ni ta position :

| Service | Ce qui est téléchargé |
|---|---|
| `media.ilevia.fr` (Ilévia, données ouvertes) | Horaires théoriques (GTFS) et état des stations V'Lille (GBFS) |
| `proxy.transport.data.gouv.fr` (point d'accès national aux données de transport) | Temps réel des bus et des trams (GTFS-RT) |
| `tiles.openfreemap.org` (OpenFreeMap, données OpenStreetMap) | Fonds de carte, uniquement quand tu ouvres la carte |

Comme toute connexion internet, ces services voient l'adresse IP de ton appareil. Pour la carte,
OpenFreeMap reçoit aussi les tuiles demandées, donc la zone affichée. Les données de transport, elles,
sont les mêmes pour tout le monde : leur téléchargement ne dit rien de ton trajet.

## La position

- Elle ne sert qu'aux questions « à proximité » et à la carte, **au moment où tu les poses**.
- Elle est demandée par Android la première fois, et tu peux la refuser : le reste de l'appli
  fonctionne normalement.
- La recherche des arrêts proches se fait **sur le téléphone**. La position n'est ni enregistrée, ni
  envoyée à un serveur, ni suivie en arrière-plan.
- Le bouton *Itinéraire* transmet le point choisi à l'appli de cartographie que tu choisis ; ce
  qu'elle en fait relève de sa propre politique.

## La voix

- **Reconnaissance vocale** : quand tu touches le micro, Chtipasse fait appel au service de
  reconnaissance vocale de ton téléphone (souvent celui de Google). Ce service peut traiter l'audio
  selon sa propre politique de confidentialité. Chtipasse ne reçoit que le texte reconnu, et il ne
  l'enregistre pas.
- **Lecture à voix haute** : elle passe par le moteur de synthèse vocale de ton téléphone.

## Les notifications

Elles affichent les réponses du widget et des raccourcis. Elles restent sur ton téléphone.

## Les assistants IA

Si tu branches Chtipasse à un assistant (serveur MCP, AppFunctions), c'est **l'assistant** qui
transmet ta question et reçoit la réponse de Chtipasse. Ce qu'il fait de ta conversation relève de
sa propre politique de confidentialité. Les outils de Chtipasse sont en lecture seule : ils ne
modifient rien.

## Enfants

Chtipasse ne collecte aucune donnée, y compris celles des enfants.

## Code ouvert

Tout ce qui est écrit ici est vérifiable : le code de Chtipasse est publié sous licence
[GPL-3.0](../LICENSE).

## Contact et changements

Pour toute question, ouvre un ticket sur le dépôt du projet. Si cette politique change, la date en
haut de page change aussi, et le changement est visible dans l'historique du dépôt.
