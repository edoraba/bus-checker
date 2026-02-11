package com.redergo.buspullman.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redergo.buspullman.data.BusRepository
import com.redergo.buspullman.data.BusUiState
import com.redergo.buspullman.data.VoiceFilter
import com.redergo.buspullman.service.UpdateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class BusViewModel : ViewModel() {

    private val repository = BusRepository()

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
        startAutoRefresh()
    }

    fun loadBusData() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val buses = repository.fetchAllBusInfo()
                _uiState.value = BusUiState.Success(
                    buses = buses,
                    lastUpdate = LocalTime.now().format(timeFormatter)
                )
            } catch (e: Exception) {
                _uiState.value = BusUiState.Error(
                    message = "Errore di connessione: ${e.localizedMessage}"
                )
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onPullToRefresh() {
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

    fun checkForUpdate(context: Context) {
        viewModelScope.launch {
            val manager = UpdateManager(context)
            _updateInfo.value = manager.checkForUpdate()
        }
    }

    fun dismissUpdate() {
        _updateInfo.value = null
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                loadBusData()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
