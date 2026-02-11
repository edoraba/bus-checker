package com.redergo.buspullman.data

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class BusRepository {

    companion object {
        private const val TAG = "BusRepository"
        private const val MAX_MINUTES = 40
        private const val MAX_RESULTS = 10
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1000L
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val api: BusApiService = Retrofit.Builder()
        .baseUrl(BusConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(BusApiService::class.java)

    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss")

    /**
     * Recupera tutte le linee monitorate da tutte le fermate configurate.
     * Le fermate vengono chiamate in parallelo. Ogni chiamata ha retry automatico.
     * Filtra solo le linee monitorate, calcola i minuti mancanti,
     * esclude i bus già passati, e ordina per minuti crescenti.
     */
    suspend fun fetchAllBusInfo(): List<BusInfo> = coroutineScope {
        val now = LocalTime.now()

        val deferreds = BusConfig.MONITORED_STOPS.map { (stopId, monitoredLines) ->
            async {
                try {
                    val passages = fetchStopWithRetry(stopId)
                    Log.d(TAG, "Fermata $stopId: ${passages.size} passaggi ricevuti")
                    val buses = passages.mapNotNull { passage ->
                        if (passage.line !in monitoredLines) return@mapNotNull null

                        val arrivalTime = LocalTime.parse(passage.hour, timeFormatter)
                        var minutesUntil = ChronoUnit.MINUTES.between(now, arrivalTime).toInt()

                        // Gestione cambio giorno: es. ora 23:50, bus alle 00:10
                        if (minutesUntil < -720) {
                            minutesUntil += 1440
                        }

                        if (minutesUntil < 0) return@mapNotNull null
                        if (minutesUntil > MAX_MINUTES) return@mapNotNull null

                        BusInfo(
                            line = passage.line,
                            minutesUntilArrival = minutesUntil,
                            isRealtime = passage.realtime,
                            stopId = stopId,
                            scheduledTime = passage.hour
                        )
                    }
                    Pair<List<BusInfo>, Exception?>(buses, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore fermata $stopId: ${e.javaClass.simpleName}: ${e.message}", e)
                    Pair<List<BusInfo>, Exception?>(emptyList(), e)
                }
            }
        }

        val results = mutableListOf<BusInfo>()
        val errors = mutableListOf<Exception>()

        deferreds.awaitAll().forEach { (buses, error) ->
            results.addAll(buses)
            error?.let { errors.add(it) }
        }

        // Se tutti gli stop hanno fallito, propaga l'errore al ViewModel
        if (results.isEmpty() && errors.isNotEmpty()) {
            throw errors.first()
        }

        results.sortedBy { it.minutesUntilArrival }.take(MAX_RESULTS)
    }

    private suspend fun fetchStopWithRetry(stopId: String): List<BusPassage> {
        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return api.getStopInfo(stopId)
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    Log.d(TAG, "Retry fermata $stopId (tentativo ${attempt + 2}/$MAX_RETRIES)")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        throw lastException!!
    }
}
