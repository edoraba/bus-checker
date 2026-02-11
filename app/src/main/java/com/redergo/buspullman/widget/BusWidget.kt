package com.redergo.buspullman.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.redergo.buspullman.MainActivity
import com.redergo.buspullman.data.BusInfo
import com.redergo.buspullman.data.BusRepository

class BusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BusRepository()
        val buses = try {
            repository.fetchAllBusInfo().take(3)
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(buses)
            }
        }
    }

    @Composable
    private fun WidgetContent(buses: List<BusInfo>) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
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
                    )
                )
            } else {
                buses.forEach { bus ->
                    WidgetBusChip(bus)
                    Spacer(modifier = GlanceModifier.width(6.dp))
                }
            }
        }
    }

    @Composable
    private fun WidgetBusChip(bus: BusInfo) {
        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
            Text(
                text = bus.line,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GlanceTheme.colors.onBackground
                )
            )
            Text(
                text = when {
                    bus.minutesUntilArrival == 0 -> "ora"
                    else -> "${bus.minutesUntilArrival}'"
                },
                style = TextStyle(
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onBackground
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
