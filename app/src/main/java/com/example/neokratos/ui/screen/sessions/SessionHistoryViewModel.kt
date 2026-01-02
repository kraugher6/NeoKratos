package com.example.neokratos.ui.screen.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.dao.WorkoutSessionDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList

class SessionHistoryViewModel(
    dao: WorkoutSessionDao
) : ViewModel() {

    val sessions = dao.getAllWorkouts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
