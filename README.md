<div align="center">

# Chtipasse

**T'as ton bus, biloute ?**

Demande à voix haute quand passe ton bus, ton tram ou ton métro, ou s'il reste un V'Lille.
Tu as la réponse en une seconde, sans compte et sans publicité.

[![Licence GPL-3.0](https://img.shields.io/badge/licence-GPL--3.0-c8102e)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84)
![Données ouvertes](https://img.shields.io/badge/donn%C3%A9es-ouvertes-0a7bbb)

</div>

<!-- Captures d'écran à venir : les déposer dans docs/images/ et les afficher ici. -->

---

## Une question, une réponse

Tu es sur le quai, le téléphone dans une main, ton sac dans l'autre. Tu touches le micro :

> 🎤 « Prochain tram à Croisé Laroche vers Roubaix »
>
> 🔊 *« Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans 1 minute, puis dans
> 13 minutes. »*

Le soir, tu rentres à vélo. Un seul bouton sur l'écran d'accueil :

> 👆 **Rentrer à vélo**
>
> Gare Lille Europe : 🚲 14 &nbsp;·&nbsp; La Madeleine Gare : 🅿️ 13

Tu sors d'un rendez-vous dans un quartier que tu connais mal :

> 🎤 « Vélo à proximité »
>
> 📍 Rihour, à 117 m : 1 vélo, 31 places &nbsp;·&nbsp; Gare Lille Flandres, à 378 m : 32 places

Pas d'itinéraire à construire, pas de menu à parcourir : **tu poses ta question, Chtipasse y
répond.**

## Pourquoi Chtipasse

**🗣️ Tu parles comme tu veux.** « Quand passe le bus dix à l'arrêt Marais direction Lille
Flandres » est compris aussi bien que « bus 10 marais ». Les fautes de frappe, les noms mal
entendus par la reconnaissance vocale (« saint mort » pour Saint-Maur), les nombres en lettres et
les mots oubliés sont rattrapés.

**🔇 La réponse suit ta façon de demander.** Si tu poses la question à voix haute, Chtipasse te
répond à voix haute. Si tu touches un bouton, la réponse est écrite et silencieuse, pratique dans
le métro ou en réunion.

**⭐ Tes trajets en un geste.** Un favori peut enchaîner plusieurs questions : les vélos au départ
*et* les places libres à l'arrivée, ou ton bus *et* ta correspondance. Chaque favori a son bouton
dans l'appli, dans le widget et dans les raccourcis de l'icône.

**🚇 Tout le réseau, une seule appli.** Bus, Lianes, tram, métro et V'Lille sont réunis. Tu peux
demander seulement les vélos disponibles (pour partir) ou seulement les places libres (pour
rendre ton vélo).

**⏱️ Honnête sur l'heure.** Chaque passage indique s'il vient du suivi en temps réel du véhicule
(badge vert) ou de l'horaire prévu (badge gris). Tu sais à quoi te fier.

**⚡ Instantané, même hors ligne.** Les horaires sont gardés sur le téléphone et mis à jour chaque
jour en arrière-plan, en Wi-Fi : la réponse arrive en moins d'un dixième de seconde. Sans réseau, Chtipasse
répond avec les horaires prévus.

**🔒 Tes trajets restent chez toi.** Chtipasse n'a ni compte, ni publicité, ni pistage, ni serveur.
Tout se calcule sur ton téléphone, et ta position ne sert qu'au moment où tu demandes ce qu'il y a
autour de toi.

**🤖 Prêt pour les assistants IA.** Claude, Antigravity et les agents du téléphone peuvent
interroger Chtipasse directement et te répondre avec les vrais horaires.

## Tout ce qu'elle sait faire

| | |
|---|---|
| **Prochains passages** | Les 3 prochains passages à un arrêt, dans chaque direction ou vers celle que tu choisis, avec le temps réel quand il existe ; le service du lendemain après le dernier passage |
| **Arrêts d'une ligne** | La liste des arrêts, dans chaque sens |
| **V'Lille** | Vélos disponibles et places libres d'une station, ou de toutes celles qui correspondent ; stations fermées signalées |
| **À proximité** | Les arrêts (avec leurs lignes) et les stations V'Lille les plus proches, filtrables par mode |
| **Carte** | Carte OpenStreetMap des arrêts et stations autour de toi, et bouton *Itinéraire* vers ton appli de navigation |
| **Voix** | Un gros bouton micro, des réponses lues en français, et une lecture que tu peux couper d'un geste |
| **Saisie assistée** | Au clavier, Chtipasse propose étape par étape le mode, la ligne, l'arrêt et la direction |
| **Favoris** | Un nom, une ou plusieurs questions ; création, ordre et modification depuis l'appli |
| **Widget** | *Parler* et un bouton par favori, avec la réponse en notification, sans ouvrir l'appli |
| **Raccourcis** | Appui long sur l'icône : *Parler* et tes favoris |
| **Aide intégrée** | Des exemples de questions, à toucher pour les essayer |
| **Thème** | Clair, sombre, ou celui du téléphone |

Le détail : [Ce que Chtipasse comprend](docs/guide/questions.md) ·
[L'appli Android](docs/guide/appli-android.md).

## Chtipasse et les autres applis

Chtipasse ne remplace ni l'appli officielle du réseau ni un calculateur d'itinéraire. Il fait une
seule chose, et il la fait vite.

| Tu veux… | Le bon outil |
|---|---|
| Aller à une adresse que tu ne connais pas, à Lille ou ailleurs | Un calculateur d'itinéraire : Google Maps, Transit, Citymapper… |
| Consulter l'information officielle du réseau (perturbations, titres de transport) | Les services d'Ilévia |
| Savoir **tout de suite** quand passe *ton* bus, *ton* tram ou *ton* métro, et s'il reste un V'Lille, à la voix ou d'un geste | **Chtipasse** |

Ce qui le distingue :

- **Conçu pour un seul réseau.** Chtipasse connaît les arrêts, les Lianes et les directions de la
  métropole lilloise. C'est ce qui lui permet de comprendre une phrase approximative au lieu
  d'exiger le nom exact d'un arrêt.
- **Une question, pas un trajet.** Il n'y a ni départ ni arrivée à saisir : tu nommes l'arrêt, tu
  as les passages.
- **Bus, tram, métro et V'Lille au même endroit**, jusque dans un même favori.
- **La voix dans les deux sens** : tu parles, il répond, sans regarder l'écran.
- **Aucune donnée personnelle collectée**, et un code ouvert que chacun peut vérifier.
- **Ouvert** : utilisable aussi dans un terminal, dans Termux et par les assistants IA.

## Ta vie privée

- Pas de compte, pas de publicité, pas de mesure d'audience, pas de serveur Chtipasse.
- Tes favoris et tes réglages restent sur ton téléphone.
- Ta position n'est demandée que pour « à proximité » et la carte. Elle est utilisée sur le
  téléphone, au moment de la question, et elle n'est jamais enregistrée.
- Les seules données téléchargées sont des données publiques : horaires, temps réel, V'Lille et
  fonds de carte.

Le détail : [politique de confidentialité](docs/confidentialite.md).

## Les limites, en toute franchise

- **Métro** : Ilévia ne publie pas de temps réel métro en données ouvertes, donc les passages du
  métro sont ceux prévus (et Chtipasse le dit).
- **V'Lille** demande une connexion ; les horaires, non.
- **Pas d'« Ok Google »** pour l'instant : il faut toucher le micro, le widget ou un raccourci.
- **Pas d'itinéraire ni d'info trafic** : ce n'est pas son rôle.

## L'installer

| Où | Comment |
|---|---|
| **Android** | Bientôt sur Google Play. En attendant : [construire l'APK](docs/dev/01-premiers-pas.md#étape-3--construire-lappli-android) |
| **Terminal** (macOS, Linux) | La commande `chti` : [guide du terminal](docs/guide/terminal.md) |
| **Termux** (Android) | `chti` et son widget Termux : [guide Termux](docs/guide/termux.md) |
| **Assistants IA** | Serveur MCP pour Claude Code, Claude Desktop, Antigravity : [guide](docs/guide/assistants-ia.md) |

```
$ chti tram croise laroche vers roubaix -n 2

TRAM — Croise Laroche  (CROISE LAROCHE 59700 MARCQ EN BAROEUL)
  → Roubaix Eurotéléport
     12:47  dans 1 min     ⏱  temps réel
     12:59  dans 13 min    ⏱  temps réel
```

## Sous le capot

Chtipasse existe en trois implémentations : un script shell sans dépendance, un cœur Kotlin
Multiplatform (JVM, Android, Node) et un serveur MCP. Elles ne divergent jamais, parce qu'un
**contrat de 122 cas figés** (une commande, une heure, la sortie exacte attendue) est joué par
chacune, **au caractère près**, à chaque modification. Le cœur suit une architecture hexagonale
dont les règles sont vérifiées automatiquement.

Envie de voir comment ça marche, ou d'y contribuer ? Le
[guide développeurs](docs/README.md#comprendre-et-contribuer) se lit en neuf chapitres courts, en
commençant par [Premiers pas](docs/dev/01-premiers-pas.md).

## Données et licence

Les horaires, le temps réel et l'état des stations V'Lille proviennent des données ouvertes
publiées par Ilévia, directement et via le
[point d'accès national transport.data.gouv.fr](https://transport.data.gouv.fr). Les fonds de carte
viennent d'[OpenStreetMap](https://www.openstreetmap.org/copyright), via
[OpenFreeMap](https://openfreemap.org).

> **Projet indépendant, non affilié à Ilévia ni à la Métropole Européenne de Lille.** Les données
> sont fournies telles quelles, sans garantie d'exactitude.

Chtipasse est un logiciel libre, publié sous licence [GPL-3.0-or-later](LICENSE).
