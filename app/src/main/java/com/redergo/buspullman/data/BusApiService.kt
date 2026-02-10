package com.redergo.buspullman.data

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the GPA MadBob API.
 * Base URL: https://gpa.madbob.org/
 */
interface BusApiService {
    @GET("query.php")
    suspend fun getStopInfo(@Query("stop") stopId: String): List<BusPassage>
}
