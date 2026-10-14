package fr.chtipasse.android

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Position du téléphone, pour « à proximité » ; l'autorisation est demandée avant par l'écran */
object DeviceLocation {
    /**
     * Position actuelle (au plus [timeoutSeconds] d'attente), sinon la dernière connue, sinon null.
     * Bloquant : à appeler hors du fil principal.
     */
    @SuppressLint("MissingPermission") // vérifiée par l'appelant (QuestionFlow)
    fun current(context: Context, timeoutSeconds: Long = 15): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }.filter { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
        val provider = providers.firstOrNull() ?: return null
        var found: Location? = null
        val done = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val cancel: CancellationSignal? = null
        LocationManagerCompat.getCurrentLocation(manager, provider, cancel, executor) { location: Location? ->
            found = location
            done.countDown()
        }
        done.await(timeoutSeconds, TimeUnit.SECONDS)
        executor.shutdown()
        return found ?: providers.mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.time }
    }
}
