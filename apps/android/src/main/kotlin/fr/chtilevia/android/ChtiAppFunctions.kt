package fr.chtilevia.android

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

/**
 * Réponse de chtilevia à un agent : en phrases à lire, et en JSON pour en tirer des faits
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ChtiAnswer(
    /** Réponse en phrases françaises, prête à être lue à voix haute (ex. « Tram, arrêt Croise Laroche, vers Roubaix Eurotéléport : dans 3 minutes. ») */
    val text: String,
    /**
     * Même réponse en JSON (schéma libs/contract/schema/response.schema.json du dépôt chtilevia) :
     * champ "type" (departures, stops, bike_stations, nearby, favorites, favorite), horaires en
     * temps réel ou non ("realtime"), vélos et places des stations V'Lille.
     */
    val json: String,
)

/**
 * AppFunctions de chtilevia : les outils du serveur MCP (apps/mcp), mêmes noms en camelCase et
 * mêmes paramètres, pour Gemini et les autres agents du téléphone. Chaque fonction lance une
 * commande chti sur le cœur de l'appli (référentiel en mémoire, favoris de l'appli).
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "ChtiAppFunctionService",
    appFunctionXmlFileName = "chti_app_function_service",
)
abstract class BaseChtiAppFunctionService : AppFunctionService() {
    private val chti by lazy { (applicationContext as ChtiApplication).chti }

    /**
     * Donne les prochains passages d'un bus, du tram ou d'un métro Ilévia (Lille) à un arrêt,
     * regroupés par destination, avec l'heure et les minutes restantes (temps réel quand le flux
     * Ilévia le permet ; le métro n'a que des horaires théoriques).
     *
     * @param mode "bus", "tram" ou "metro".
     * @param arret Tout ou partie du nom de l'arrêt (ex. "gambetta", "croisé laroche") ; fautes
     *   d'orthographe et de reconnaissance vocale tolérées.
     * @param ligne Ligne : pour le bus, son nom affiché ("10", "L1", "CO3", "Z2") ; pour le métro,
     *   "1" ou "2". Inutile pour le tram (une seule ligne).
     * @param direction Tout ou partie du nom de la destination (ex. "flandres", "roubaix", "chu") ;
     *   toutes les directions si absente.
     * @param nombre Passages par direction, de 1 à 20 ; 0 pour la valeur par défaut (3).
     * @return Les passages, en phrases et en JSON (type "departures").
     * @throws AppFunctionElementNotFoundException Ligne, arrêt ou direction inconnus : le message
     *   liste les choix possibles (arrêts proches de la demande, directions de la ligne) à proposer
     *   à l'utilisateur.
     * @throws AppFunctionInvalidArgumentException Paramètre manquant ou invalide.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun prochainsPassages(
        mode: String,
        arret: String,
        ligne: String? = null,
        direction: String? = null,
        nombre: Int = 0,
    ): ChtiAnswer = answer(ChtiCommands.departures(mode, ligne, arret, direction, nombre))

    /**
     * Liste dans l'ordre les arrêts d'une ligne Ilévia pour chaque sens, éventuellement pour une
     * seule direction.
     *
     * @param mode "bus", "tram" ou "metro".
     * @param ligne Ligne : pour le bus, son nom affiché ("10", "L1") ; pour le métro, "1" ou "2".
     *   Inutile pour le tram.
     * @param direction Tout ou partie du nom de la destination ; tous les sens si absente.
     * @return Les arrêts, en phrases et en JSON (type "stops").
     * @throws AppFunctionElementNotFoundException Ligne ou direction inconnues.
     * @throws AppFunctionInvalidArgumentException Ligne manquante pour le bus ou le métro.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listerArrets(
        mode: String,
        ligne: String? = null,
        direction: String? = null,
    ): ChtiAnswer = answer(ChtiCommands.stops(mode, ligne, direction), voice = false)

    /**
     * Donne les vélos disponibles et les places libres des stations V'Lille dont le nom ressemble
     * à la recherche, avec leur état (hors service, location ou dépôt possible).
     *
     * @param station Tout ou partie du nom de la station (ex. "rihour", "gare lille europe") ;
     *   fautes tolérées.
     * @param mode Intention : "prendre" un vélo, en "deposer" un, ou "tout" (par défaut).
     * @return Les stations, en phrases et en JSON (type "bike_stations").
     * @throws AppFunctionElementNotFoundException Aucune station ne ressemble à la recherche.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun stationVelo(
        station: String,
        mode: String? = null,
    ): ChtiAnswer = answer(ChtiCommands.bikeStations(station, mode))

    /**
     * Trouve les arrêts Ilévia les plus proches d'une position (avec les lignes qui les
     * desservent) et les stations V'Lille les plus proches (avec vélos et places), triés par
     * distance à vol d'oiseau. Enchaîner avec "prochainsPassages" pour les horaires d'un arrêt.
     *
     * @param latitude Latitude en degrés décimaux (ex. 50.6366).
     * @param longitude Longitude en degrés décimaux (ex. 3.0630).
     * @param nombre Arrêts et stations à donner, de 1 à 10 ; 0 pour la valeur par défaut (3).
     * @param mode Seulement ces arrêts ("bus", "tram", "metro") ou seulement les stations V'Lille
     *   ("velo") ; tout si absent.
     * @return Les arrêts et stations, en phrases et en JSON (type "nearby").
     * @throws AppFunctionInvalidArgumentException Position ou mode invalides.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun autourDeMoi(
        latitude: Double,
        longitude: Double,
        nombre: Int = 0,
        mode: String? = null,
    ): ChtiAnswer = answer(ChtiCommands.nearby(latitude, longitude, nombre, mode))

    /**
     * Liste les trajets favoris de l'utilisateur : un nom court et les questions qu'il pose.
     *
     * @return Les favoris, en phrases et en JSON (type "favorites").
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listerFavoris(): ChtiAnswer = answer(ChtiCommands.favorites())

    /**
     * Lance un trajet favori de l'utilisateur et donne le résultat de chacune de ses questions
     * (passages, vélos…).
     * Required workflow: Call "listerFavoris" first to get the exact favorite names.
     *
     * @param nom Nom du favori, tel que donné par "listerFavoris" (ex. "travail").
     * @return Les réponses du favori, en phrases et en JSON (type "favorite").
     * @throws AppFunctionElementNotFoundException Favori inconnu : appeler "listerFavoris".
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun favori(nom: String): ChtiAnswer = withContext(Dispatchers.IO) {
        val known = chti.favoriteNames()
        // chti <nom> lancerait aussi une commande (update, vocal…) : seulement un vrai favori
        if (known.none { it.lowercase(Locale.ROOT) == nom.trim().lowercase(Locale.ROOT) }) {
            throw AppFunctionElementNotFoundException("favori inconnu : $nom (favoris : ${known.joinToString(", ")})")
        }
        answer(listOf(nom.trim()))
    }

    /**
     * Lance [args] en JSON puis en phrases ([voice]) ou en texte (à l'oral, une ligne sans arrêt
     * demande l'arrêt au lieu de lister) ; une erreur de chti devient l'exception qui l'explique
     */
    private suspend fun answer(args: List<String>, voice: Boolean = true): ChtiAnswer = withContext(Dispatchers.IO) {
        val json = chti.run(listOf("--json") + args)
        val document = runCatching { JSONObject(json.out) }.getOrNull()
            ?: throw AppFunctionAppUnknownException("réponse illisible de chti : ${json.text.take(200)}")
        if (document.optString("type") == "error") throw ChtiCommands.exception(document)
        ChtiAnswer(text = chti.run((if (voice) listOf("--voix") else emptyList()) + args).out.trim(), json = document.toString())
    }
}
