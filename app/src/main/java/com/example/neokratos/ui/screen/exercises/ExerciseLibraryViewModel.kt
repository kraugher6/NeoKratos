package com.example.neokratos.ui.screen.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.*

class ExerciseLibraryViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ExerciseCategory?>(null)
    val selectedCategory: StateFlow<ExerciseCategory?> = _selectedCategory.asStateFlow()

    private val allExercises: Flow<List<ExerciseEntity>> =
        exerciseRepository.allExercises

    val filteredExercises: StateFlow<List<ExerciseEntity>> = combine(
        allExercises,
        searchQuery,
        selectedCategory
    ) { exercises, query, category ->
        exercises
            .filter { exercise ->
                if (category != null && exercise.category != category) {
                    return@filter false
                }
                if (query.isNotBlank()) {
                    exercise.name.contains(query, ignoreCase = true)
                } else {
                    true
                }
            }
            .sortedBy { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: ExerciseCategory?) {
        _selectedCategory.value = category
    }
}