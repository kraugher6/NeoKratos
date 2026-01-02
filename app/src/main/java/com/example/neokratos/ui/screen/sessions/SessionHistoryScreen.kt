package com.example.neokratos.ui.screen.sessions

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SessionHistoryScreen(viewModel: SessionHistoryViewModel) {
    val sessions by viewModel.sessions.collectAsState()

    LazyColumn {
        items(sessions) {
            Text("Session ${it.id} - ${it.startTime}")
        }
    }
}
