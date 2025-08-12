package eu.swpelc.boardsimulator.ui.buildings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.swpelc.boardsimulator.data.AppDatabase
import eu.swpelc.boardsimulator.model.Building
import eu.swpelc.boardsimulator.model.BuildingItem
import eu.swpelc.boardsimulator.repository.BuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BuildingsViewModel(private val repository: BuildingRepository) : ViewModel() {
    
    val buildings = repository.getAllBuildings()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun addBuilding(building: Building, name: String, description: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val buildingItem = BuildingItem(
                    building = building,
                    name = name,
                    description = description
                )
                repository.insertBuilding(buildingItem)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateBuilding(buildingItem: BuildingItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateBuilding(buildingItem)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteBuilding(buildingItem: BuildingItem) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteBuilding(buildingItem)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class BuildingsViewModelFactory(private val repository: BuildingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuildingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BuildingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
