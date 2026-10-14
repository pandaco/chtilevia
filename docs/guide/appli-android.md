# L'appli Chtipasse sur Android

Une appli légère pour savoir **quand passe ton bus, ton tram ou ton métro** et **s'il reste un
V'Lille**, à la voix ou en un geste. Tout se calcule sur le téléphone : il n'y a ni serveur, ni
compte, ni publicité.

Il faut Android 8.0 ou une version plus récente.

## Installer

- **Google Play** : bientôt disponible.
- **Depuis les sources** : voir [Premiers pas](../dev/01-premiers-pas.md#étape-3--construire-lappli-android)
  (une commande pour construire l'APK, une pour l'installer).

Au premier lancement, l'appli télécharge les horaires du réseau, ce qui prend quelques secondes. Ils
occupent environ 44 Mo sur le téléphone.

## L'écran principal

Au centre, un **gros bouton micro**. Touche-le et pose ta question : « prochain tram à Croisé
Laroche vers Roubaix », « vélo à proximité », « travail »… La réponse est lue à voix haute et
s'affiche en même temps.

- **Écrire** : pour taper ta question. La saisie est assistée : l'appli te propose, étape par étape,
  le mode, la ligne, l'arrêt, la direction ou la station V'Lille, tirés des vrais horaires.
- **Les réponses en cartes** : une carte par arrêt ou par station. Les passages s'affichent en
  badges, **verts** pour le temps réel et **gris** pour l'horaire théorique. Touche une carte pour
  voir l'arrêt ou la station sur la carte.
- **Corriger une question** : touche la question affichée pour la modifier au clavier.
- **🔊 / 🔇** : coupe la lecture à voix haute. La réponse reste écrite, et ce réglage est gardé.
- **☀ / ☾** : thème clair ou sombre. Tant que tu n'as rien choisi, l'appli suit le thème du
  téléphone.
- **Aide** (*Que demander ?*) : des exemples de questions, à toucher pour les essayer.

Tu ne sais pas quoi demander ? Toutes les formulations comprises sont décrites dans
[Ce que Chtipasse comprend](questions.md).

## Les favoris : tes trajets en un geste

Un favori donne un nom court à une ou plusieurs questions. Il apparaît sous forme de bouton au-dessus
de la barre du bas.

Exemples :

- **Travail** → « tram Croisé Laroche vers Roubaix », 2 passages ;
- **Rentrer à vélo** → « vélos à Gare Lille Europe » **puis** « places libres à La Madeleine
  Gare ». Les deux réponses arrivent d'un seul geste.

*Modifier* ouvre l'écran des favoris, où tu peux :

- créer un favori : son nom (c'est le texte du bouton, et ce que tu dis au micro pour le lancer),
  puis ses questions, tapées avec la même saisie assistée ;
- modifier ou retirer une question ;
- changer l'ordre des boutons (c'est aussi l'ordre du widget) ;
- supprimer un favori.

Un favori touché répond **par écrit**, sans bruit. Le même favori dit au micro répond **à voix
haute**.

## Le widget d'accueil

Appui long sur l'écran d'accueil → **Widgets** → **Chtipasse**.

- **Parler** (le bouton rouge) écoute ta question et lit la réponse.
- **Un bouton par favori** affiche la réponse, sans voix.

La réponse arrive en **notification**, sans ouvrir l'appli. Touche la notification pour l'ouvrir
dans l'appli. Le widget suit tes favoris : en ajouter un ajoute un bouton.

## Les raccourcis de l'icône

Appui long sur l'icône de Chtipasse : **Parler**, puis un raccourci par favori (autant que le
lanceur en accepte). Tu peux les glisser sur l'écran d'accueil.

## À proximité et carte

« À proximité », « vélo à proximité », « bus autour de moi »… donnent les arrêts et stations les plus
proches de toi, avec leur distance. La première fois, l'appli demande l'accès à la position.

Le bouton **Carte** affiche une carte OpenStreetMap avec :

- les arrêts et stations proches, filtrables par mode (bus, tram, métro, V'Lille) ;
- une liste touchable, du plus proche au plus lointain ;
- **Me localiser**, pour recentrer sur toi ;
- **Itinéraire**, qui ouvre l'appli de cartographie de ton choix pour t'y guider.

## Toujours à l'heure, sans y penser

- Les horaires sont **mis à jour chaque jour en arrière-plan**, en Wi-Fi et quand la batterie est
  suffisante : une question n'attend jamais un téléchargement.
- Ils restent **en mémoire** tant que l'appli tourne, et la réponse arrive alors en moins d'un
  dixième de seconde.
- **Sans réseau**, l'appli répond avec les horaires théoriques déjà téléchargés. V'Lille, en
  revanche, demande une connexion.

## Les autorisations, et pourquoi

| Autorisation | À quoi elle sert | Quand elle est demandée |
|---|---|---|
| Internet | Télécharger les horaires, le temps réel, l'état des stations V'Lille et les fonds de carte | — |
| Position | Les questions « à proximité » et la carte | À la première question « à proximité » |
| Notifications | Afficher les réponses du widget et des raccourcis | Au premier bouton du widget |

Le micro passe par la reconnaissance vocale du téléphone : l'appli ne demande pas elle-même l'accès
au micro. Le détail est dans la [politique de confidentialité](../confidentialite.md).

## Avec les agents du téléphone (Android 16 et plus)

L'appli expose ses fonctions (prochains passages, arrêts d'une ligne, V'Lille, à proximité, favoris)
aux agents du téléphone comme Gemini, grâce à **AppFunctions**. Gemini ne les appelle pour l'instant
que pour les testeurs de son programme d'accès anticipé : voir [Assistants IA](assistants-ia.md).

## Questions fréquentes

| Question | Réponse |
|---|---|
| Pourquoi le métro est-il « théorique » ? | Ilévia ne publie pas de temps réel métro en données ouvertes. Les horaires du métro sont donc ceux prévus. |
| Puis-je dire « Ok Google, quand passe mon bus » ? | Pas encore : il faut toucher le widget *Parler*, le micro de l'appli ou un raccourci. |
| La réponse est lue en anglais | Installe la voix française : Paramètres → Synthèse vocale → Installer les données vocales → Français. |
| « Je n'ai pas compris » | Commence par le mode : « bus 10… », « métro 1… », « tram… », « vélo… », ou un nom de favori. |
| Les heures sont décalées | Vérifie le fuseau horaire du téléphone (Paramètres → Date et heure). |
| L'appli lit les réponses dans le bus ! | Touche 🔊 pour couper la voix, ou utilise les boutons de favoris : ils répondent par écrit. |

---

Suite : [Ce que Chtipasse comprend](questions.md) · [Confidentialité](../confidentialite.md)
