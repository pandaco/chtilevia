# Chtipasse dans Termux — guide d'installation

> Tu veux simplement l'appli ? Elle s'installe en un geste et n'a besoin de rien d'autre :
> voir [l'appli Android](appli-android.md). Ce guide s'adresse à qui préfère tout garder dans
> [Termux](https://termux.dev), avec le script `chti` et le widget Termux:Widget.

Ce guide explique comment installer `chti` dans Termux sur un téléphone Android, puis comment l'utiliser au
quotidien : **à la voix** ou **en un clic** depuis l'écran d'accueil. Tout tourne sur le téléphone :
pas de serveur, pas de compte, pas d'abonnement.

> Ce guide n'a pas encore été déroulé sur un vrai téléphone, seulement avec des commandes Termux
> simulées (`apps/shell/tests/termux.sh`). Si une étape ne se passe pas comme décrit, voir [Dépannage](#dépannage).

## Ce que tu obtiens

Un widget sur l'écran d'accueil, avec un bouton par action :

| Bouton | Tu fais… | Tu obtiens… |
|---|---|---|
| **Parler** | Tu touches, puis tu dis ta question : « métro 1 République vers CHU » | La réponse **lue à voix haute**, et en notification |
| **Travail** | Tu touches, c'est tout | La réponse **écrite** à l'écran, et en notification, sans voix |
| **Rentrer à vélo** | Tu touches, c'est tout | Vélos dispo au départ et places libres à l'arrivée, **écrits** |

La règle est simple : **si tu demandes à la voix, chti répond à la voix ; si tu touches un bouton,
chti répond par écrit.**

Exemple de réponse : *« Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans 1 minute,
puis dans 13 minutes. »*

## Installation (une seule fois, ~15 minutes)

### 1. Installer les 3 applis Termux depuis F-Droid

Les versions du Play Store sont anciennes et incompatibles entre elles : il faut celles de F-Droid.

1. Sur le téléphone, ouvre [f-droid.org](https://f-droid.org) et installe l'appli **F-Droid**
   (Android demande d'autoriser l'installation depuis le navigateur : accepte).
2. Dans F-Droid, installe : **Termux**, **Termux:API** et **Termux:Widget**.
3. Ouvre **Termux** une première fois et mets-le à jour :
   ```sh
   pkg upgrade -y
   ```
   (s'il pose des questions, la réponse par défaut convient : touche Entrée).

### 2. Copier le projet sur le téléphone

Le plus discret : passer par une archive, sans rien publier sur internet.

1. **Sur ton ordinateur**, dans le dossier du projet :
   ```sh
   git archive --format=zip -o chtipasse.zip HEAD
   ```
2. Copie `chtipasse.zip` dans le dossier **Téléchargements** du téléphone (câble USB, Google Drive,
   e-mail à toi-même…).
3. **Dans Termux**, donne l'accès aux fichiers du téléphone (Android demande une autorisation :
   accepte), puis décompresse :
   ```sh
   termux-setup-storage
   pkg install -y unzip
   unzip ~/storage/downloads/chtipasse.zip -d ~/chtipasse
   ```

Si le projet est sur un dépôt git (GitHub…), tu peux aussi faire :
`pkg install -y git && git clone <url-du-dépôt> ~/chtipasse`.

### 3. Lancer l'installeur

De préférence en Wi-Fi (il télécharge ~9 Mo d'horaires) :

```sh
sh ~/chtipasse/apps/shell/termux/install.sh
```

Il affiche 5 étapes :

1. installation des outils nécessaires (`python`, `curl`, `jq`…) ;
2. création des commandes `chti` et `chti-widget` ;
3. copie des **favoris d'exemple** dans `~/.config/chtipasse/favoris` (si tu n'en as pas déjà) ;
4. installation des 3 boutons du widget ;
5. téléchargement et préparation des horaires (quelques dizaines de secondes).

### 4. Mettre tes propres trajets dans les favoris

Les favoris fournis sont des exemples. Remplace-les par tes trajets :

```sh
nano ~/.config/chtipasse/favoris
```

Le format est `nom: commande`, une commande par ligne :

```
travail:      tram croise laroche vers roubaix -n 2
rentrer-velo: velo gare lille europe --prendre
rentrer-velo: velo la madeleine gare --deposer
```

- La commande est celle que tu taperais après `chti` (voir [la commande `chti`](terminal.md#toutes-les-commandes)).
- Un même nom sur plusieurs lignes enchaîne les commandes : c'est ce qui permet d'avoir les vélos
  au départ **et** les places à l'arrivée.
- Pour trouver le nom exact d'un arrêt : `chti bus l1` liste les arrêts de la Liane 1.
- Pour vérifier une direction : `chti bus l1 -d wambrechies` montre les arrêts dans cette direction ;
  une direction inconnue affiche la liste des directions possibles.

Dans `nano`, la touche **CTRL** est dans la barre de touches au-dessus du clavier de Termux :
**CTRL + O** puis **Entrée** pour enregistrer, **CTRL + X** pour quitter.

Teste ensuite dans Termux :

```sh
chti favoris        # liste tes favoris
chti travail        # lance le favori « travail »
```

Ce fichier reste sur ton téléphone : il n'est pas dans le projet et n'est jamais publié.

### 5. Ajouter le widget sur l'écran d'accueil

1. Appui long sur une zone vide de l'écran d'accueil → **Widgets**.
2. Cherche **Termux:Widget** et pose le widget « liste » : il affiche les 3 boutons.
   Le widget « raccourci » (1 × 1) pose un seul bouton, si tu préfères une icône par action.
3. Si la liste est vide, touche l'icône de rafraîchissement du widget.

### 6. Premier essai

1. Touche **Travail** : la réponse s'affiche en haut de l'écran et dans les notifications.
   Si Android demande d'autoriser les notifications pour Termux:API, accepte.
2. Touche **Parler**, attends une seconde, puis dis : « métro 1 République ».
   La première fois, Android demande l'accès au micro pour Termux:API : accepte.

## Au quotidien

### Demander à la voix (bouton Parler)

Commence ta phrase par le **mode**, puis l'arrêt, puis éventuellement « vers » et la direction :

| Tu dis… | chti comprend |
|---|---|
| « Prochain bus 10 à Gambetta vers Lille Flandres » | bus 10, arrêt Gambetta, direction Lille Flandres |
| « Quand passe le bus dix à l'arrêt Gambetta » | bus 10, arrêt Gambetta, toutes directions |
| « Métro un Gare Lille Flandres direction CHU » | métro 1, direction CHU |
| « Le prochain tram à Croisé Laroche vers Roubaix » | tram, direction Roubaix |
| « Liane 1 République » | bus L1, arrêt République Beaux-Arts, toutes directions |
| « Vélo Rihour » | vélos et places libres à la station Rihour |
| « V'Lille Gare Lille Europe pour déposer » | seulement les places libres |
| « Travail », « Rentrer à vélo » | le favori du même nom (réponse à la voix) |

Astuces :

- Les lignes peuvent être dites en chiffres ou en lettres jusqu'à vingt (« dix », « douze »).
- Un morceau du nom d'arrêt suffit (« flandres » pour « Gare Lille Flandres »).
- Si la direction est ambiguë, sois plus précis : « vers Lille Flandres » plutôt que « vers Lille ».
- Si chti ne comprend pas, il le dit et donne un exemple de phrase.

### Consulter en un clic (boutons de favoris)

Touche **Travail** ou **Rentrer à vélo** : la réponse s'affiche à l'écran quelques secondes, puis
reste dans les notifications. Aucun son : pratique dans les transports ou au bureau.

### Ajouter un bouton pour un autre favori

Par exemple pour un favori `sport` déjà présent dans ton fichier de favoris :

```sh
printf '#!%s/bin/sh\nchti-widget --ecrit sport\n' "$PREFIX" > ~/.shortcuts/tasks/Sport
chmod +x ~/.shortcuts/tasks/Sport
```

Le bouton **Sport** apparaît dans le widget (rafraîchis-le au besoin). Remplace `--ecrit` par
`--parle` si tu préfères que ce bouton réponde à la voix.

### Dans le terminal Termux

Toutes les [commandes de `chti`](terminal.md#toutes-les-commandes) marchent aussi dans Termux :
`chti bus l1 republique`, `chti velo rihour`, `chti metro 1 republique -d chu`…

## Mettre à jour

- **Les horaires** se mettent à jour tout seuls, une fois par jour, au premier usage.
  Pour forcer : `chti update`.
- **Le projet** : recopie la nouvelle archive comme à l'étape 2 (en remplaçant l'ancien dossier :
  `rm -rf ~/chtipasse` avant `unzip`), puis relance `sh ~/chtipasse/apps/shell/termux/install.sh`.
  Tes favoris sont conservés.

## Dépannage

| Symptôme | Que faire |
|---|---|
| Rien ne se passe quand on touche **Parler** | Vérifie que **Termux:API** est installée (depuis F-Droid) et qu'elle a l'autorisation **micro** : Paramètres Android → Applis → Termux:API → Autorisations. |
| Les boutons n'affichent rien | Autorise les **notifications** pour Termux:API : Paramètres Android → Applis → Termux:API → Notifications. |
| Le widget est vide | Touche l'icône de rafraîchissement du widget. Vérifie que `ls ~/.shortcuts/tasks` liste bien les boutons, sinon relance l'installeur. |
| La réponse est lue en anglais | Installe la voix française : Paramètres Android → Synthèse vocale → moteur Google → Installer les données vocales → Français. |
| « Je n'ai pas compris » | Commence par le mode : « bus 10… », « métro 1… », « tram… », « vélo… », ou un nom de favori. |
| « Aucune direction ne correspond » | La réponse donne les directions possibles : reformule avec l'une d'elles. |
| « L'arrêt … n'est pas desservi … en direction de … » | L'arrêt existe, mais la ligne n'y passe que dans d'autres directions (souvent une ligne à sens unique à cet endroit) : la réponse les cite. |
| Le premier usage de la journée est lent | Normal : les horaires sont retéléchargés et préparés une fois par jour (quelques secondes). |
| Les heures sont décalées | Vérifie le fuseau horaire du téléphone (Paramètres → Date et heure). |
| `chti : commande requise introuvable` | Un outil manque : relance `sh ~/chtipasse/apps/shell/termux/install.sh`. |

## Désinstaller

```sh
rm -rf ~/chtipasse ~/.config/chtipasse ~/.cache/chtipasse
rm -f "$PREFIX/bin/chti" "$PREFIX/bin/chti-widget"
rm -f ~/.shortcuts/tasks/Parler ~/.shortcuts/tasks/Travail ~/.shortcuts/tasks/"Rentrer à vélo"
```

Puis désinstalle Termux, Termux:API et Termux:Widget si tu ne t'en sers pas pour autre chose.
