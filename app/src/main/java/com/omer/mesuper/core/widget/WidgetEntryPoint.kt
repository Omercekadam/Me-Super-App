package com.omer.mesuper.core.widget

import com.omer.mesuper.feature.agenda.data.AgendaRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Glance widget'ı Hilt component ağacının dışında olduğu için repository'ye bu yoldan erişir. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun agendaRepository(): AgendaRepository
}
