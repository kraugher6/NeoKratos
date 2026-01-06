package com.example.neokratos.ui.screen.activeworkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neokratos.data.local.ExerciseCategory
import com.example.neokratos.data.local.entity.ExerciseEntity
import com.example.neokratos.data.repository.ExerciseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/**
 * ViewModel for ExercisePickerDialog.
 *
 * Manages:
 * - Exercise list from repository
 * - Search filtering
 * - Category filtering
 * - Combined filtering logic
 */
class ExercisePickerViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    // ===== STATE =====

    /**
     * Current search query.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Currently selected category filter.
     * null = show all categories.
     */
    private val _selectedCategory = MutableStateFlow<ExerciseCategory?>(null)
    val selectedCategory: StateFlow<ExerciseCategory?> = _selectedCategory.asStateFlow()

    /**
     * All exercises from repository.
     */
    private val allExercises: Flow<List<ExerciseEntity>> =
        exerciseRepository.allExercises

    /**
     * Filtered exercises based on search and category.
     *
     * Combines:
     * - Search query (name contains)
     * - Category filter
     *
     * Updates automatically when search or category changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredExercises: StateFlow<List<ExerciseEntity>> = combine(
        allExercises,
        searchQuery,
        selectedCategory
    ) { exercises, query, category ->
        exercises
            .filter { exercise ->
                // Filter by category
                if (category != null && exercise.category != category) {
                    return@filter false
                }

                // Filter by search query
                if (query.isNotBlank()) {
                    exercise.name.contains(query, ignoreCase = true)
                } else {
                    true
                }
            }
            .sortedBy { it.name } // Sort alphabetically
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ===== ACTIONS =====

    /**
     * Update search query.
     * Triggers automatic refiltering.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Select a category filter.
     * Pass null to show all categories.
     */
    fun selectCategory(category: ExerciseCategory?) {
        _selectedCategory.value = category
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
    }
}

/**
 * Factory for ExercisePickerViewModel.
 *
 * Note: Since this is used inside a Dialog and needs repository,
 * we'll need to pass it from parent or use a different approach.
 *
 * For now, we'll create a simpler version that gets repository
 * from a static holder (not ideal but works for MVP).
 */