package com.redergo.buspullman.service

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.redergo.buspullman.BuildConfig
import com.redergo.buspullman.data.BusConfig
import com.redergo.buspullman.data.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.File

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubRelease
}

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"

        /** Elimina i vecchi APK BusPullman dalla cartella Downloads */
        fun cleanOldApks() {
            try {
                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                downloads.listFiles()?.filter {
                    it.name.startsWith("BusPullman-") && it.name.endsWith(".apk")
                }?.forEach { it.delete() }
            } catch (_: Exception) { }
        }
    }

    private val api = Retrofit.Builder()
        .baseUrl(BusConfig.GITHUB_API_BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GitHubApiService::class.java)

    data class UpdateInfo(
        val newVersion: String,
        val downloadUrl: String,
        val releaseNotes: String?
    )

    /**
     * Controlla se è disponibile una versione più recente.
     * Restituisce UpdateInfo se c'è un aggiornamento, null altrimenti.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = BuildConfig.VERSION_NAME
            Log.d(TAG, "Check aggiornamento... versione corrente: $currentVersion")

            val release = api.getLatestRelease(
                BusConfig.GITHUB_REPO_OWNER,
                BusConfig.GITHUB_REPO_NAME
            )

            val latestVersion = release.tagName.removePrefix("v")
            Log.d(TAG, "Ultima release: $latestVersion (tag: ${release.tagName}), assets: ${release.assets.size}")
            release.assets.forEach { asset ->
                Log.d(TAG, "  Asset: ${asset.name}")
            }

            if (isNewerVersion(latestVersion, currentVersion)) {
                val apkAsset = release.assets.firstOrNull {
                    it.name.endsWith(".apk")
                }
                if (apkAsset != null) {
                    Log.i(TAG, "Aggiornamento disponibile: $latestVersion, APK: ${apkAsset.name}")
                    UpdateInfo(
                        newVersion = latestVersion,
                        downloadUrl = apkAsset.downloadUrl,
                        releaseNotes = release.body
                    )
                } else {
                    Log.w(TAG, "Versione $latestVersion trovata ma nessun .apk nella release!")
                    null
                }
            } else {
                Log.d(TAG, "Già aggiornato ($currentVersion >= $latestVersion)")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Check aggiornamento fallito: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * Confronto semantico versioni: "1.2.0" > "1.1.0"
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    /**
     * Scarica l'APK tramite DownloadManager e avvia l'installazione.
     */
    fun downloadAndInstall(downloadUrl: String, version: String) {
        val fileName = "BusPullman-$version.apk"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Aggiornamento BusPullman v$version")
            .setDescription("Download in corso...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE)
            as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)
                    installApk(fileName)
                }
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(fileName: String) {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
