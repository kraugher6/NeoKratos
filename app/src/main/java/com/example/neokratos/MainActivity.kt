package com.example.neokratos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neokratos.data.local.GymDatabase
import com.example.neokratos.data.repository.WorkoutSessionRepository
import com.example.neokratos.data.repository.WorkoutTemplateRepository
import com.example.neokratos.ui.screen.home.HomeScreen
import com.example.neokratos.ui.screen.templates.TemplateListScreen
import com.example.neokratos.ui.screen.templates.TemplateManageScreen
import com.example.neokratos.ui.screen.templates.TemplateViewModel
import com.example.neokratos.ui.screen.history.HistoryScreen
import com.example.neokratos.ui.screen.history.HistoryViewModel
import com.example.neokratos.ui.screen.history.HistoryViewModelFactory
import com.example.neokratos.ui.theme.NeoKratosTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = GymDatabase.getInstance(this)

        val workoutSessionRepository = WorkoutSessionRepository(
            database.workoutSessionDao()
        )

        val templateRepository = WorkoutTemplateRepository(
            database.workoutTemplateDao()
        )

        setContent {
            NeoKratosTheme(dynamicColor = false) {

                val templateViewModel: TemplateViewModel = viewModel {
                    TemplateViewModel(database.workoutTemplateDao())
                }

                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModelFactory(workoutSessionRepository)
                )

                HomeScreen(
                    templatesScreen = {
                        TemplateListScreen(viewModel = templateViewModel)
                    },
                    manageTemplatesScreen = {
                        TemplateManageScreen(viewModel = templateViewModel)
                    },
                    historyScreen = {
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onBack = {} // per ora non serve
                        )
                    }
                )
            }
        }
    }
}
