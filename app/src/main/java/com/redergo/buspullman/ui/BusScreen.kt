package com.redergo.buspullman.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redergo.buspullman.BuildConfig
import com.redergo.buspullman.data.BusInfo
import com.redergo.buspullman.data.BusUiState
import com.redergo.buspullman.service.UpdateManager

// Colori per linea
private val lineColors = mapOf(
    "15" to Color(0xFF1E88E5),  // Blu
    "68" to Color(0xFFE53935),  // Rosso
    "61" to Color(0xFF43A047)   // Verde
)

private fun getLineColor(line: String): Color =
    lineColors[line] ?: Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusScreen(
    viewModel: BusViewModel,
    isListening: Boolean = false,
    onMicClick: () -> Unit = {},
    onSpeak: (String) -> Unit = {},
    onDownloadUpdate: (UpdateManager.UpdateInfo) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceFilter by viewModel.voiceFilter.collectAsStateWithLifecycle()
    val shouldSpeak by viewModel.shouldSpeak.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()

    val displayedBuses = when (val state = uiState) {
        is BusUiState.Success -> {
            val filter = voiceFilter.requestedLine
            if (filter != null) {
                state.buses.filter { bus -> bus.line == filter }
            } else {
                state.buses
            }
        }
        else -> emptyList()
    }

    LaunchedEffect(shouldSpeak) {
        if (shouldSpeak && uiState is BusUiState.Success) {
            val text = buildTtsText(displayedBuses, voiceFilter.requestedLine)
            onSpeak(text)
            viewModel.onSpeakConsumed()
        }
    }

    val fabColor by animateColorAsState(
        targetValue = if (isListening)
            MaterialTheme.colorScheme.error
        else
            MaterialTheme.colorScheme.primary,
        label = "fab_color"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("BusPullman", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (uiState is BusUiState.Success) {
                            Text(
                                "Aggiornato alle ${(uiState as BusUiState.Success).lastUpdate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onPullToRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = fabColor
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Microfono",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is BusUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Caricamento orari...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is BusUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadBusData() }) {
                                Text("Riprova")
                            }
                        }
                    }
                }

                is BusUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        // Banner aggiornamento
                        val update = updateInfo
                        if (update != null) {
                            item(key = "update_banner") {
                                UpdateBanner(
                                    info = update,
                                    onUpdate = { onDownloadUpdate(update) },
                                    onDismiss = { viewModel.dismissUpdate() }
                                )
                            }
                        }

                        // Filtro vocale attivo
                        val filterLine = voiceFilter.requestedLine
                        if (filterLine != null) {
                            item(key = "filter_bar") {
                                FilterChipBar(
                                    line = filterLine,
                                    onClear = { viewModel.setVoiceFilter(null) }
                                )
                            }
                        }

                        if (displayedBuses.isEmpty()) {
                            item(key = "empty") {
                                val hour = java.time.LocalTime.now().hour
                                val isNight = hour in 0..4 || hour >= 23
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when {
                                            filterLine != null -> "Nessun passaggio previsto per il $filterLine"
                                            isNight -> "Servizio terminato\nRiprova domani mattina"
                                            else -> "Nessun passaggio previsto"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(
                            items = displayedBuses,
                            key = { bus: BusInfo -> "${bus.line}_${bus.scheduledTime}" }
                        ) { bus: BusInfo ->
                            BusArrivalCard(bus)
                        }

                        // Bottone "Leggi orari"
                        if (displayedBuses.isNotEmpty()) {
                            item(key = "speak_button") {
                                Spacer(Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = { viewModel.requestSpeak() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Leggi orari")
                                }
                            }
                        }

                        // Spazio in fondo per il FAB
                        item(key = "spacer") { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun BusArrivalCard(busInfo: BusInfo) {
    val lineColor = getLineColor(busInfo.line)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = lineColor.copy(alpha = 0.1f),
                spotColor = lineColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numero linea in cerchio colorato
            Surface(
                shape = CircleShape,
                color = lineColor,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = busInfo.line,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        busInfo.minutesUntilArrival == 0 -> "In arrivo"
                        busInfo.minutesUntilArrival == 1 -> "1 minuto"
                        else -> "${busInfo.minutesUntilArrival} minuti"
                    },
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (busInfo.minutesUntilArrival <= 2)
                        lineColor
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Previsto alle ${busInfo.scheduledTime.substringBeforeLast(":")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge GPS / Orario
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (busInfo.isRealtime)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.outlineVariant
            ) {
                Text(
                    text = if (busInfo.isRealtime) "GPS" else "Orario",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (busInfo.isRealtime)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FilterChipBar(line: String, onClear: () -> Unit) {
    val lineColor = getLineColor(line)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = lineColor.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = lineColor,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Solo linea $line",
                style = MaterialTheme.typography.labelLarge,
                color = lineColor,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onClear) {
                Text("Mostra tutte")
            }
        }
    }
}

@Composable
fun UpdateBanner(
    info: UpdateManager.UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Aggiornamento disponibile v${info.newVersion}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                info.releaseNotes?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Button(onClick = onUpdate) {
                    Text("Aggiorna")
                }
                TextButton(onClick = onDismiss) {
                    Text("Ignora")
                }
            }
        }
    }
}

fun buildTtsText(buses: List<BusInfo>, requestedLine: String?): String {
    if (buses.isEmpty()) {
        return if (requestedLine != null) {
            "Nessun passaggio previsto per il $requestedLine"
        } else {
            "Nessun passaggio previsto"
        }
    }

    return buses.joinToString(". ") { bus ->
        when {
            bus.minutesUntilArrival == 0 -> "${bus.line} in arrivo"
            bus.minutesUntilArrival == 1 -> "${bus.line} tra 1 minuto"
            else -> "${bus.line} tra ${bus.minutesUntilArrival} minuti"
        }
    }
}
