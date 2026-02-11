package com.redergo.buspullman

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redergo.buspullman.data.BusConfig
import com.redergo.buspullman.service.UpdateManager
import com.redergo.buspullman.ui.BusScreen
import com.redergo.buspullman.ui.BusViewModel
import com.redergo.buspullman.ui.theme.BusPullmanTheme
import com.redergo.buspullman.widget.BusWidgetWorker
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var busViewModel: BusViewModel

    // Text-to-Speech
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Speech-to-Text
    private var speechRecognizer: SpeechRecognizer? = null

    // Permesso microfono
    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Permesso microfono negato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        busViewModel = ViewModelProvider(this)[BusViewModel::class.java]
        busViewModel.setContext(this)

        // Inizializza TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ITALIAN
                ttsReady = true
            }
        }

        // Check aggiornamenti
        busViewModel.checkForUpdate(this)

        // Avvia refresh periodico widget
        BusWidgetWorker.enqueuePeriodicRefresh(this)

        setContent {
            BusPullmanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isListening by busViewModel.isListening.collectAsStateWithLifecycle()

                    BusScreen(
                        viewModel = busViewModel,
                        isListening = isListening,
                        onMicClick = {
                            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                startSpeechRecognition()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onSpeak = { text ->
                            if (ttsReady) {
                                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "bus_info")
                            }
                        },
                        onDownloadUpdate = { info ->
                            UpdateManager(this@MainActivity)
                                .downloadAndInstall(info.downloadUrl, info.newVersion)
                        }
                    )
                }
            }
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Riconoscimento vocale non disponibile", Toast.LENGTH_SHORT).show()
            return
        }

        // Ferma TTS se sta parlando
        tts?.stop()

        speechRecognizer?.destroy()
        busViewModel.setListening(true)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    busViewModel.setListening(false)
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    handleVoiceInput(spokenText)
                }

                override fun onError(error: Int) {
                    busViewModel.setListening(false)
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Non ho capito, riprova"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Nessun audio rilevato"
                        else -> "Errore riconoscimento vocale"
                    }
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun handleVoiceInput(spokenText: String) {
        val monitoredLines = BusConfig.MONITORED_STOPS.values.flatten()

        // Cerca un numero di linea monitorata nel testo
        val foundLine = monitoredLines.firstOrNull { line ->
            spokenText.contains(line)
        }

        busViewModel.setVoiceFilter(foundLine)
        busViewModel.loadBusData()
        busViewModel.requestSpeak()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
