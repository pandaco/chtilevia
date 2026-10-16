# Chtipasse

Prochains passages des transports en commun et disponibilité des vélos en libre-service d'un réseau,
à partir de ses données ouvertes. Lille (Ilévia, V'Lille) est le premier réseau.

## Réseau

**Réseau** :
Ensemble des lignes, arrêts et stations vélos d'un territoire, décrit par ses données ouvertes et son fuseau horaire.
_Avoid_ : Opérateur, Ilévia (c'est un réseau parmi d'autres)

**Ligne** :
Service identifié par un nom court affiché aux voyageurs (« 10 », « TRAM », « M1 ») et un mode.
_Avoid_ : Route

**Mode** :
Type de transport d'une ligne : bus, tram ou métro. Les Lianes sont des bus.
_Avoid_ : Type de ligne

**Course** :
Trajet d'un véhicule sur une ligne, d'un bout à l'autre, un jour de service donné.
_Avoid_ : Trip, voyage, service

**Arrêt** :
Lieu nommé où les voyageurs montent, tel qu'ils le désignent (« Marais ») ; il regroupe un ou plusieurs quais.
_Avoid_ : Zone d'arrêt, station (réservé aux vélos), stop

**Quai** :
Point physique d'un arrêt où le véhicule s'arrête, souvent un par sens de circulation.
_Avoid_ : Arrêt physique, poteau

**Direction** :
Libellé affiché par une course pour indiquer où elle va (« Lille Flandres ») ; c'est par lui que le voyageur filtre ses passages.
_Avoid_ : Destination, terminus, headsign, sens

**Référentiel** :
Description théorique complète du réseau : lignes, arrêts, courses, horaires et calendrier.
_Avoid_ : GTFS, base, données statiques

## Temps

**Jour de service** :
Journée d'exploitation à laquelle appartient une course ; une course qui roule après minuit appartient à la veille.
_Avoid_ : Date, jour calendaire

**Passage** :
Moment où une course dessert un arrêt, tel qu'annoncé au voyageur.
_Avoid_ : Départ, horaire

**Passage théorique** :
Passage tiré du référentiel, sans information sur la position réelle du véhicule.
_Avoid_ : Horaire prévu, planifié

**Temps réel** :
Prévision issue du suivi des véhicules, qui remplace le passage théorique d'une course suivie.
_Avoid_ : Live, en direct

**Propagation du retard** :
Report du dernier retard connu d'une course sur ses arrêts suivants, que le temps réel ne détaille pas.

**Course annulée** :
Course que le temps réel signale comme supprimée ; elle n'a plus aucun passage.

## Vélos

**Station** :
Point de location V'Lille, avec une capacité et une disponibilité. Le mot est réservé aux vélos.
_Avoid_ : Borne

**Disponibilité** :
État d'une station à un instant : vélos à prendre, places pour déposer, et si elle est en service.

**Hors service** :
Station qui n'est ni installée, ni ouverte à la location ou au dépôt.

## Usage

**Recherche** :
Ce que le voyageur demande, dit ou écrit, en langage libre ou sous forme de commande.
_Avoid_ : Question, requête, phrase, query

**Commande** :
Forme canonique d'une recherche, en mots-clés (`bus 10 marais -d flandres`), vers laquelle toute phrase libre est traduite.
_Avoid_ : Ligne de commande, instruction

**Recherche floue** :
Tolérance d'une recherche aux noms approximatifs (fautes, accents, phonétique, reconnaissance vocale) ; en cas d'égalité, tous les candidats sont proposés.
_Avoid_ : Correspondance approximative, fuzzy match

**Favori** :
Nom donné par le voyageur à une suite de questions, jouées dans l'ordre.
_Avoid_ : Raccourci, trajet enregistré

**À proximité** :
Arrêts et stations les plus proches d'une position, à vol d'oiseau.
_Avoid_ : Autour (c'est le mot de la commande, pas le concept)
