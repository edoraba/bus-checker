package com.redergo.buspullman.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
import com.redergo.buspullman.MainActivity
import com.redergo.buspullman.R
import com.redergo.buspullman.data.BusInfo
import com.redergo.buspullman.data.BusRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(6.dp)
                .background(GlanceTheme.colors.background)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            if (buses.isEmpty()) {
                Text(
                    text = "Nessun bus",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onBackground
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            } else {
                buses.forEach { bus ->
                    WidgetBusChip(bus)
                    Spacer(modifier = GlanceModifier.width(4.dp))
                }
                Spacer(modifier = GlanceModifier.defaultWeight())
            }

            Column(
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                // Bottone refresh
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Aggiorna",
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                    )
                }
                Text(
                    text = updateTime,
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = GlanceTheme.colors.onBackground
                    )
                )
            }
        }
    }

    @Composable
    private fun WidgetBusChip(bus: BusInfo) {
        Column(
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(GlanceTheme.colors.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = bus.line,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = GlanceTheme.colors.onSecondaryContainer
                )
            )
            Text(
                text = when {
                    bus.minutesUntilArrival == 0 -> "ora"
                    else -> "${bus.minutesUntilArrival}'"
                },
                style = TextStyle(
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = GlanceTheme.colors.onSecondaryContainer
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
