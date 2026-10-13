package fr.chtilevia.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.chtilevia.application.NearbyMode
import fr.chtilevia.domain.Position
import fr.chtilevia.presentation.distance
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.Locale

/** Repère de la carte : un arrêt (couleur de son mode) ou une station V'Lille, avec ce qu'on en dit dans la liste */
data class MapPoint(
    val id: String,
    val position: Position,
    val label: String,
    val kind: Kind,
    /** « Métro 1, Bus P1 » ou « 🚲 3 · 🅿️ 5 » */
    val detail: String,
    /** Distance depuis toi, inconnue sans ta position */
    val distance: Int?,
) {
    enum class Kind(val color: Color, val legend: String) {
        ME(Color(0xFF1A73E8), "Toi"),
        BUS(Color(0xFF1E6FD9), "Bus"),
        TRAM(Color(0xFF2E9E48), "Tram"),
        METRO(Color(0xFFF08A00), "Métro"),
        BIKE(Brand.red, "V'Lille"),
    }
}

/** Ce que montre la carte : [points] autour de [me], le repère [selected] mis en avant */
data class MapState(
    val mode: NearbyMode,
    val me: Position? = null,
    val points: List<MapPoint> = emptyList(),
    val selected: String? = null,
    val message: String? = "Je cherche ta position…",
)

/**
 * Carte OpenStreetMap (MapLibre, tuiles OpenFreeMap : sans clé ni compte) des arrêts et stations
 * proches, avec la liste en dessous : toucher un emplacement (dans la liste ou sur la carte) le
 * met en avant et propose l'itinéraire, dans l'appli de cartographie du téléphone.
 */
@Composable
fun MapScreen(state: MapState, onMode: (NearbyMode) -> Unit, onSelect: (String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Autour de toi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Fermer") }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((mode, label) in MODES) {
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val map = remember { mutableStateOf<MapLibreMap?>(null) }
            OsmMap(state, onSelect, Modifier.fillMaxSize()) { map.value = it }
            // Me localiser : la carte revient sur le point bleu
            SmallFloatingActionButton(
                onClick = { map.value?.let { recenter(it, state.me) } },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) { Icon(painterResource(R.drawable.ic_my_location), contentDescription = "Me localiser") }
            state.message?.let {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (state.me == null) CircularProgressIndicator()
                    Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(16.dp))
                }
            }
        }
        // Légende des couleurs
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (kind in MapPoint.Kind.entries) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).background(kind.color, CircleShape))
                    Text(kind.legend, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        // Les emplacements, du plus proche au plus lointain
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
            items(state.points, key = { it.id }) { point ->
                PlaceRow(point, selected = point.id == state.selected, onClick = { onSelect(point.id) }) { openInMapsApp(context, point) }
            }
        }
    }
}

@Composable
private fun PlaceRow(point: MapPoint, selected: Boolean, onClick: () -> Unit, onDirections: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    Column(Modifier.fillMaxWidth().background(background).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(point.kind.color, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(point.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(point.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            point.distance?.let { Text(distance(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (selected) {
            TextButton(onClick = onDirections, modifier = Modifier.padding(start = 14.dp)) { IconLabel(R.drawable.ic_directions, "Itinéraire") }
        }
    }
}

/**
 * Itinéraire jusqu'au repère : l'appli de cartographie installée sur le téléphone, quelle qu'elle
 * soit (lien « geo: » standard ; Android propose le choix s'il y en a plusieurs)
 */
private fun openInMapsApp(context: Context, point: MapPoint) {
    val at = "%.6f,%.6f".format(Locale.ROOT, point.position.latitude, point.position.longitude)
    val uri = "geo:$at?q=$at(${Uri.encode(point.label)})".toUri()
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Aucune appli de cartographie sur ce téléphone.", Toast.LENGTH_SHORT).show()
    }
}

private val MODES = listOf(
    NearbyMode.ALL to "Tout",
    NearbyMode.BUS to "🚌 Bus",
    NearbyMode.TRAM to "🚊 Tram",
    NearbyMode.METRO to "🚇 Métro",
    NearbyMode.BIKE to "🚲 V'Lille",
)

private const val STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val SOURCE = "reperes"
private const val CIRCLES = "reperes-cercles"

@Composable
private fun OsmMap(state: MapState, onSelect: (String?) -> Unit, modifier: Modifier, onReady: (MapLibreMap) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val select by rememberUpdatedState(onSelect)
    val view = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    // Cadrage sur l'ensemble seulement quand les repères changent, pas à chaque sélection
    val framed = remember { mutableStateOf<List<MapPoint>?>(null) }
    // La carte suit le cycle de vie de l'écran (rendu, mémoire)
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            view.onDestroy()
        }
    }
    AndroidView({ view }, modifier) { mapView ->
        mapView.getMapAsync { map ->
            val style = map.style
            if (style == null) {
                map.setStyle(Style.Builder().fromUri(STYLE)) {
                    showMe(context, map, it)
                    // Toucher un repère le sélectionne ; toucher ailleurs annule la sélection
                    map.addOnMapClickListener { at ->
                        val touched = map.queryRenderedFeatures(map.projection.toScreenLocation(at), CIRCLES).firstOrNull()
                        select(touched?.getStringProperty("id"))
                        true
                    }
                    onReady(map)
                    show(map, it, state, frame = true)
                    framed.value = state.points
                }
            } else {
                val frame = framed.value != state.points
                show(map, style, state, frame)
                if (frame) framed.value = state.points
            }
        }
    }
}

/** Point bleu de localisation (avec sa précision), mis à jour quand tu bouges ; seulement si c'est autorisé */
@SuppressLint("MissingPermission") // vérifiée juste avant
private fun showMe(context: Context, map: MapLibreMap, style: Style) {
    val allowed = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!allowed) return
    map.locationComponent.apply {
        activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
        isLocationComponentEnabled = true
        renderMode = RenderMode.COMPASS
    }
}

/** Revient sur ta position (dernière connue du point bleu, sinon celle de la recherche) */
private fun recenter(map: MapLibreMap, me: Position?) {
    val here = map.locationComponent.takeIf { it.isLocationComponentActivated }?.lastKnownLocation
        ?.let { LatLng(it.latitude, it.longitude) }
        ?: me?.let { LatLng(it.latitude, it.longitude) }
        ?: return
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 16.0))
}

/**
 * Repères en cercles de la couleur de leur mode, avec leur nom ; le repère sélectionné plus gros et
 * cerclé de noir, la vue centrée dessus. Sinon, si [frame], la vue cadrée sur toi et les repères.
 */
private fun show(map: MapLibreMap, style: Style, state: MapState, frame: Boolean) {
    val features = FeatureCollection.fromFeatures(
        state.points.map { point ->
            Feature.fromGeometry(Point.fromLngLat(point.position.longitude, point.position.latitude)).apply {
                addStringProperty("id", point.id)
                addStringProperty("label", point.label)
                // La couleur voyage avec le repère : pas de correspondance à maintenir côté carte
                addStringProperty("color", "#%06X".format(point.kind.color.toArgb() and 0xFFFFFF))
                addBooleanProperty("selected", point.id == state.selected)
            }
        },
    )
    val source = style.getSourceAs<GeoJsonSource>(SOURCE)
    if (source != null) {
        source.setGeoJson(features)
    } else {
        style.addSource(GeoJsonSource(SOURCE, features))
        val selected = Expression.get("selected")
        style.addLayer(
            CircleLayer(CIRCLES, SOURCE).withProperties(
                PropertyFactory.circleRadius(Expression.switchCase(selected, Expression.literal(13f), Expression.literal(8f))),
                PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
                PropertyFactory.circleStrokeColor(
                    Expression.switchCase(selected, Expression.color(Brand.ink.toArgb()), Expression.color(AndroidColor.WHITE)),
                ),
                PropertyFactory.circleStrokeWidth(Expression.switchCase(selected, Expression.literal(4f), Expression.literal(2.5f))),
            ),
        )
        style.addLayer(
            SymbolLayer("reperes-noms", SOURCE).withProperties(
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
                PropertyFactory.textSize(13f),
                PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
                PropertyFactory.textAnchor("top"),
                PropertyFactory.textColor(Brand.ink.toArgb()),
                PropertyFactory.textHaloColor(AndroidColor.WHITE),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textAllowOverlap(false),
            ),
        )
    }
    val focus = state.points.firstOrNull { it.id == state.selected }
    val all = listOfNotNull(state.me) + state.points.map { it.position }
    when {
        focus != null -> map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(focus.position.latitude, focus.position.longitude), 17.0))
        !frame -> Unit
        all.size >= 2 -> {
            val bounds = LatLngBounds.Builder().apply { all.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
            map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
        all.size == 1 -> map.easeCamera(CameraUpdateFactory.newLatLngZoom(LatLng(all[0].latitude, all[0].longitude), 15.0))
        else -> map.cameraPosition = CameraPosition.Builder().target(LatLng(50.6366, 3.063)).zoom(11.0).build()
    }
}
