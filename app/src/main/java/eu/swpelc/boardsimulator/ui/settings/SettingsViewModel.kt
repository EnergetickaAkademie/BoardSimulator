package eu.swpelc.boardsimulator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import eu.swpelc.boardsimulator.model.LoginResponse
import eu.swpelc.boardsimulator.model.ServerSettings
import eu.swpelc.boardsimulator.model.SimulationDump
import eu.swpelc.boardsimulator.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    
    val serverSettings = repository.serverSettings
    val loginToken = repository.loginToken
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _loginResult = MutableStateFlow<Result<LoginResponse>?>(null)
    val loginResult: StateFlow<Result<LoginResponse>?> = _loginResult.asStateFlow()
    
    private val _simulationDump = MutableStateFlow<SimulationDump?>(null)
    val simulationDump: StateFlow<SimulationDump?> = _simulationDump.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun saveSettings(settings: ServerSettings) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.saveSettings(settings)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun login() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.login()
                _loginResult.value = result
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun logout() {
        repository.logout()
        _loginResult.value = null
        _simulationDump.value = null
    }
    
    fun fetchSimulationDump() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val result = repository.getSimulationDump()
                if (result.isSuccess) {
                    _simulationDump.value = result.getOrNull()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to fetch simulation dump"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return repository.isLoggedIn()
    }
    
    fun clearLoginResult() {
        _loginResult.value = null
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
