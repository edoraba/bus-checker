package com.redergo.buspullman.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.redergo.buspullman.MainActivity
import com.redergo.buspullman.R
import com.redergo.buspullman.data.BusInfo
import com.redergo.buspullman.data.BusRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// Colori per linea nel widget
private object LineColors {
    val text = mapOf(
        "15" to ColorProvider(Color(0xFF64B5F6)),
        "68" to ColorProvider(Color(0xFFEF5350)),
        "61" to ColorProvider(Color(0xFF66BB6A))
    )
    val defaultText = ColorProvider(Color(0xFFBDBDBD))

    val chipBg = mapOf(
        "15" to ColorProvider(Color(0x401565C0)),
        "68" to ColorProvider(Color(0x40C62828)),
        "61" to ColorProvider(Color(0x402E7D32))
    )
    val defaultChipBg = ColorProvider(Color(0x40757575))
}

class BusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BusRepository()
        val buses = try {
            repository.fetchAllBusInfo().take(3)
        } catch (e: Exception) {
            emptyList()
        }
        val updateTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        provideContent {
            GlanceTheme {
                WidgetContent(buses, updateTime)
            }
        }
    }

    @Composable
    private fun WidgetContent(buses: List<BusInfo>, updateTime: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(Color(0xFF1A1C2E)))
                .clickable(actionStartActivity<MainActivity>())
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            // Riga superiore: chip bus
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                if (buses.isEmpty()) {
                    Text(
                        text = "Nessun bus",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = ColorProvider(Color(0xCCFFFFFF))
                        )
                    )
                } else {
                    buses.forEachIndexed { index, bus ->
                        WidgetBusChip(bus)
                        if (index < buses.size - 1) {
                            Spacer(modifier = GlanceModifier.width(4.dp))
                        }
                    }
                }
            }

            // Riga inferiore: timestamp + refresh
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = updateTime,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = ColorProvider(Color(0x99FFFFFF))
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Box(
                    modifier = GlanceModifier
                        .size(24.dp)
                        .cornerRadius(12.dp)
                        .background(ColorProvider(Color(0x33FFFFFF)))
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Aggiorna",
                        modifier = GlanceModifier.size(14.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xCCFFFFFF)))
                    )
                }
            }
        }
    }

    @Composable
    private fun WidgetBusChip(bus: BusInfo) {
        Column(
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(LineColors.chipBg[bus.line] ?: LineColors.defaultChipBg)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = bus.line,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = LineColors.text[bus.line] ?: LineColors.defaultText
                )
            )
            Text(
                text = when {
                    bus.minutesUntilArrival == 0 -> "ora"
                    else -> "${bus.minutesUntilArrival}'"
                },
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = ColorProvider(Color(0xFFFFFFFF))
                )
            )
        }
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        BusWidget().update(context, glanceId)
    }
}

class BusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = BusWidget()
}
