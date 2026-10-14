# Ce que Chtipasse comprend

Chtipasse répond à quatre familles de questions. Tu peux les poser à la voix, les taper dans
l'appli, les mettre dans un favori ou les écrire dans un terminal : partout, la même phrase donne la
même réponse.

> **En une phrase :** commence par le mode (bus, tram, métro, vélo), donne la ligne et l'arrêt,
> ajoute « vers » et la direction si tu veux. Le reste, Chtipasse le devine.

## 1. Prochains passages

Le mode, la ligne, l'arrêt et, si tu veux, la direction après « vers » :

| Tu dis ou tu écris… | Chtipasse comprend |
|---|---|
| « Prochain tram à Croisé Laroche vers Roubaix » | tram, arrêt Croisé Laroche, direction Roubaix |
| « Bus 10 Gambetta » | bus 10, arrêt Gambetta, toutes les directions |
| « Quand passe le bus dix à l'arrêt Marais direction Lille Flandres » | bus 10, arrêt Marais, direction Lille Flandres |
| « Métro un République vers CHU » | métro 1, arrêt République Beaux-Arts, direction CHU |
| « Liane 1 République » | bus L1, arrêt République Beaux-Arts |

Tu obtiens les trois prochains passages dans chaque direction (deux quand la réponse est lue à voix
haute). Pour chacun, tu sais s'il vient du **suivi en temps réel** du véhicule ou de l'**horaire
théorique**. S'il n'y a plus de service ce soir, les premiers départs du lendemain s'affichent.

Sans arrêt (« bus 10 »), tu obtiens la liste des arrêts de la ligne, dans chaque sens.

## 2. V'Lille

Le nombre de vélos disponibles et de places libres d'une station :

| Tu dis ou tu écris… | Chtipasse répond |
|---|---|
| « Vélo Rihour » | vélos et places libres à Rihour |
| « V'Lille Gare Lille Europe pour prendre » | seulement les vélos disponibles |
| « Vélo Sainte Hélène pour déposer » | seulement les places libres |

« prendre » et « emprunter » donnent les vélos ; « déposer », « rendre » et « poser » donnent les
places libres. Une station fermée à la location ou au dépôt est signalée.

## 3. À proximité

Les arrêts les plus proches de toi (avec les lignes qui y passent) et les stations V'Lille les plus
proches, à vol d'oiseau :

| Tu dis ou tu écris… | Chtipasse montre |
|---|---|
| « À proximité », « autour de moi », « près d'ici » | arrêts et stations, tous modes |
| « Vélo à proximité » | seulement les stations V'Lille |
| « Bus autour de moi », « le métro le plus proche » | seulement les arrêts de ce mode |

L'appli Android utilise la position du téléphone, avec ton accord, et uniquement au moment de la
question. Dans un terminal, tu donnes toi-même la position : voir [`chti autour`](terminal.md#à-proximité).

## 4. Favoris

Dis simplement le nom d'un favori : « Travail », « Rentrer à vélo ». Un favori peut enchaîner
plusieurs questions, par exemple les vélos disponibles au départ **et** les places libres à
l'arrivée. Les petits mots sont ignorés : « rentrer à vélo » trouve le favori `rentrer-velo`.

## Les fautes sont pardonnées

Personne ne tape « République Beaux-Arts » correctement à un arrêt de bus, sous la pluie, d'une main.
Chtipasse ne demande pas l'orthographe exacte :

- **Un morceau suffit** : « flandres » trouve « Gare Lille Flandres ».
- **Casse, accents et ponctuation sont ignorés** : « croise laroche » = « Croisé Laroche ».
- **Les fautes de frappe et de reconnaissance vocale sont rattrapées**, à l'écrit comme au son :
  « gambeta », « croix la roche », « saint mort ».
- **Mots collés, séparés ou manquants** : « eglise marais » trouve « Église du Marais ».
- **Nombres en lettres** : « quatre cantons » trouve « 4 Cantons », « bus dix » donne la ligne 10
  (de un à vingt).
- **Mots d'introduction** : « quand passe le prochain… », « à l'arrêt… » sont ignorés en début de
  phrase, mais jamais au milieu d'un nom (« Pont de Bois » reste intact).

Chtipasse ne choisit pas à ta place quand il hésite : si deux arrêts ressemblent autant à ta
demande, il te les propose. Si une direction n'existe pas, il te donne la liste des directions
possibles. Si l'arrêt existe mais que la ligne n'y passe pas dans ce sens, il te le dit, avec les
directions qui le desservent.

## Comment Chtipasse répond

La réponse suit la façon de demander :

- **Question dite au micro** : réponse lue à voix haute, et écrite.
- **Bouton touché ou question tapée** : réponse écrite seulement, discrète dans les transports ou au
  bureau.

Exemple de réponse lue : *« Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans
1 minute, puis dans 13 minutes. »* Au-delà d'une heure, c'est l'heure qui est donnée
(« à 7 h 42 ») ; un horaire sans suivi en temps réel est suivi de « (prévu) ».

Si la phrase n'est pas comprise, Chtipasse le dit et propose un exemple à reformuler.

---

Suite : [l'appli Android](appli-android.md) · [le terminal](terminal.md) ·
[les assistants IA](assistants-ia.md)
