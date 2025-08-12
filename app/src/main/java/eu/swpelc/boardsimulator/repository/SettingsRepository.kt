package eu.swpelc.boardsimulator.repository

import android.content.Context
import android.content.SharedPreferences
import eu.swpelc.boardsimulator.model.LoginResponse
import eu.swpelc.boardsimulator.model.ServerSettings
import eu.swpelc.boardsimulator.model.SimulationDump
import eu.swpelc.boardsimulator.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val apiService = ApiService()
    
    private val _serverSettings = MutableStateFlow(loadSettings())
    val serverSettings: Flow<ServerSettings> = _serverSettings.asStateFlow()
    
    private val _loginToken = MutableStateFlow<String?>(loadLoginToken())
    val loginToken: Flow<String?> = _loginToken.asStateFlow()
    
    private fun loadSettings(): ServerSettings {
        val serverIp = prefs.getString("server_ip", "") ?: ""
        val lecturerUsername = prefs.getString("lecturer_username", "lecturer1") ?: "lecturer1"
        val lecturerPassword = prefs.getString("lecturer_password", "lecturer123") ?: "lecturer123"
        val boardUsernames = prefs.getString("board_usernames", "board1, board2, board3") ?: "board1, board2, board3"
        val refreshInterval = prefs.getLong("refresh_interval", 10000L)
        
        return ServerSettings(
            serverIp = serverIp,
            lecturerUsername = lecturerUsername,
            lecturerPassword = lecturerPassword,
            boardUsernames = boardUsernames,
            refreshInterval = refreshInterval
        )
    }
    
    private fun loadLoginToken(): String? {
        return prefs.getString("login_token", null)
    }
    
    fun saveSettings(settings: ServerSettings) {
        prefs.edit().apply {
            putString("server_ip", settings.serverIp)
            putString("lecturer_username", settings.lecturerUsername)
            putString("lecturer_password", settings.lecturerPassword)
            putString("board_usernames", settings.boardUsernames)
            putLong("refresh_interval", settings.refreshInterval)
            apply()
        }
        _serverSettings.value = settings
    }
    
    suspend fun login(): Result<LoginResponse> {
        val settings = _serverSettings.value
        return try {
            val result = apiService.login(settings.serverIp, settings.lecturerUsername, settings.lecturerPassword)
            if (result.isSuccess) {
                val token = result.getOrNull()?.token
                _loginToken.value = token
                // Persist the token
                prefs.edit().putString("login_token", token).apply()
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSimulationDump(): Result<SimulationDump> {
        val settings = _serverSettings.value
        val token = _loginToken.value
        
        return if (token != null) {
            apiService.getSimulationDump(settings.serverIp, token)
        } else {
            Result.failure(Exception("Not logged in. Please login first."))
        }
    }
    
    fun logout() {
        _loginToken.value = null
        // Remove the token from SharedPreferences
        prefs.edit().remove("login_token").apply()
    }
    
    fun isLoggedIn(): Boolean {
        return _loginToken.value != null
    }
}
