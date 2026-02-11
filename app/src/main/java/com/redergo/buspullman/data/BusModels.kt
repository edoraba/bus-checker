package com.redergo.buspullman.data

import com.google.gson.annotations.SerializedName

/**
 * Represents a single bus passage from the GPA API.
 *
 * Example response from https://gpa.madbob.org/query.php?stop=578:
 * {"line":"15","hour":"21:07:53","realtime":true}
 */
data class BusPassage(
    @SerializedName("line") val line: String,
    @SerializedName("hour") val hour: String,
    @SerializedName("realtime") val realtime: Boolean
)

/**
 * Processed bus info ready for display/TTS.
 */
data class BusInfo(
    val line: String,
    val minutesUntilArrival: Int,
    val isRealtime: Boolean,
    val stopId: String,
    val scheduledTime: String
)

/**
 * Configuration for monitored bus stops and lines.
 */
object BusConfig {
    // Stop 578: Lines 15 and 68
    // Stop 1805: Line 61
    val MONITORED_STOPS = mapOf(
        "578" to listOf("15", "68"),
        "1805" to listOf("61")
    )

    const val API_BASE_URL = "https://gpa.madbob.org/"

    // GitHub auto-update (da configurare con il repo reale)
    const val GITHUB_REPO_OWNER = "edoraba"
    const val GITHUB_REPO_NAME = "bus-checker"
    const val GITHUB_API_BASE_URL = "https://api.github.com/"
}

/**
 * Stato UI per la schermata principale.
 */
sealed interface BusUiState {
    data object Loading : BusUiState
    data class Success(
        val buses: List<BusInfo>,
        val lastUpdate: String
    ) : BusUiState
    data class Error(val message: String) : BusUiState
}

/**
 * Filtro vocale: quale linea è stata richiesta.
 * null = mostra tutte le linee.
 */
data class VoiceFilter(
    val requestedLine: String? = null
)

/**
 * Modelli per GitHub Releases API (auto-update).
 */
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("assets") val assets: List<GitHubAsset>,
    @SerializedName("body") val body: String?
)

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)
