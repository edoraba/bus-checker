package com.redergo.buspullman.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redergo.buspullman.data.BusRepository
import com.redergo.buspullman.data.BusUiState
import com.redergo.buspullman.data.VoiceFilter
import com.redergo.buspullman.service.UpdateManager
import com.redergo.buspullman.widget.BusWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BusViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository = BusRepository()
    private val updateManager = UpdateManager(appContext)

    private val _uiState = MutableStateFlow<BusUiState>(BusUiState.Loading)
    val uiState: StateFlow<BusUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _voiceFilter = MutableStateFlow(VoiceFilter())
    val voiceFilter: StateFlow<VoiceFilter> = _voiceFilter.asStateFlow()

    private val _shouldSpeak = MutableStateFlow(false)
    val shouldSpeak: StateFlow<Boolean> = _shouldSpeak.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Auto-update
    private val _updateInfo = MutableStateFlow<UpdateManager.UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateManager.UpdateInfo?> = _updateInfo.asStateFlow()

    private var autoRefreshJob: Job? = null
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    init {
        loadBusData()
    }

    fun loadBusData() {
        if (_isRefreshing.value) return // Deduplicazione: ignora se già in corso
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val buses = repository.fetchAllBusInfo()
                _uiState.value = BusUiState.Success(
                    buses = buses,
                    lastUpdate = LocalTime.now().format(timeFormatter)
                )
                // Aggiorna anche il widget
                BusWidget().updateAll(appContext)
            } catch (e: Exception) {
                val current = _uiState.value
                if (current is BusUiState.Success) {
                    // Stale-while-revalidate: mantieni dati vecchi, segnala offline
                    _uiState.value = current.copy(isOffline = true)
                } else {
                    val message = when {
                        e is UnknownHostException || e is java.net.ConnectException ->
                            "Nessuna connessione internet"
                        e is java.net.SocketTimeoutException ->
                            "Il server non risponde"
                        else ->
                            "Errore di connessione: ${e.localizedMessage}"
                    }
                    _uiState.value = BusUiState.Error(message = message)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onPullToRefresh() {
        _voiceFilter.value = VoiceFilter()
        loadBusData()
    }

    fun setVoiceFilter(line: String?) {
        _voiceFilter.value = VoiceFilter(requestedLine = line)
    }

    fun requestSpeak() {
        _shouldSpeak.value = true
    }

    fun onSpeakConsumed() {
        _shouldSpeak.value = false
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
    }

    fun checkForUpdate() {
        // Non ri-checkare se il banner è già visibile
        if (_updateInfo.value != null) return
        viewModelScope.launch {
            _updateInfo.value = updateManager.checkForUpdate()
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    // Lifecycle-aware: chiama da onResume
    fun startAutoRefresh() {
        // Ri-controlla aggiornamenti ad ogni resume
        checkForUpdate()
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                loadBusData()
            }
        }
    }

    // Lifecycle-aware: chiama da onPause
    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
