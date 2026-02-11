package com.redergo.buspullman.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class BusRepository {

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
     * Filtra solo le linee monitorate, calcola i minuti mancanti,
     * esclude i bus già passati, e ordina per minuti crescenti.
     */
    suspend fun fetchAllBusInfo(): List<BusInfo> {
        val now = LocalTime.now()
        val results = mutableListOf<BusInfo>()

        for ((stopId, monitoredLines) in BusConfig.MONITORED_STOPS) {
            try {
                val passages = api.getStopInfo(stopId)
                for (passage in passages) {
                    if (passage.line !in monitoredLines) continue

                    val arrivalTime = LocalTime.parse(passage.hour, timeFormatter)
                    var minutesUntil = ChronoUnit.MINUTES.between(now, arrivalTime).toInt()

                    // Gestione cambio giorno: es. ora 23:50, bus alle 00:10
                    // between() restituisce un valore molto negativo (~-1420)
                    // Se è più negativo di -720 (12 ore), il bus è dopo mezzanotte
                    if (minutesUntil < -720) {
                        minutesUntil += 1440 // aggiungi 24 ore
                    }

                    if (minutesUntil < 0) continue // già passato

                    results.add(
                        BusInfo(
                            line = passage.line,
                            minutesUntilArrival = minutesUntil,
                            isRealtime = passage.realtime,
                            stopId = stopId,
                            scheduledTime = passage.hour
                        )
                    )
                }
            } catch (e: Exception) {
                // Se una fermata fallisce, continua con le altre
                e.printStackTrace()
            }
        }

        return results.sortedBy { it.minutesUntilArrival }
    }
}
