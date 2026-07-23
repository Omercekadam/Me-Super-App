package com.omer.mesuper.core.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.omer.mesuper.MainActivity
import com.omer.mesuper.feature.agenda.data.AgendaRepository
import com.omer.mesuper.feature.agenda.data.TaskEntity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private fun agendaRepositoryOf(context: Context): AgendaRepository =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        .agendaRepository()

class MeSuperWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = agendaRepositoryOf(context)
        val today = LocalDate.now()
        val habitTotal = repository.habits.first().size
        val habitTicked = repository.ticks.first().count { it.date == today }
        val openTasks = repository.tasks.first().filter { !it.isDone }.sortedBy { it.dueDate }.take(4)

        provideContent {
            GlanceTheme {
                WidgetContent(habitTicked = habitTicked, habitTotal = habitTotal, openTasks = openTasks)
            }
        }
    }
}

class MeSuperWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MeSuperWidget()
}

private val TaskIdKey = ActionParameters.Key<Long>("taskId")

class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val repository = agendaRepositoryOf(context)
        val task = repository.tasks.first().firstOrNull { it.id == taskId } ?: return
        repository.setTaskDone(task, true)
        MeSuperWidget().updateAll(context)
    }
}

@Composable
private fun WidgetContent(habitTicked: Int, habitTotal: Int, openTasks: List<TaskEntity>) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        Text(
            "Me SuperApp — Bugün",
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            if (habitTotal > 0) "🔗 Zincir: $habitTicked/$habitTotal" else "🔗 Alışkanlık yok",
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (openTasks.isEmpty()) {
            Text("Açık görev yok 🎉", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        } else {
            openTasks.forEach { task ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(actionRunCallback<CompleteTaskAction>(actionParametersOf(TaskIdKey to task.id))),
                ) {
                    Text("☐ ${task.title}", style = TextStyle(color = GlanceTheme.colors.onSurface))
                }
            }
        }
    }
}
