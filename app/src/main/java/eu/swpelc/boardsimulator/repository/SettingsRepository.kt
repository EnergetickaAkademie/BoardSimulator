package eu.swpelc.boardsimulator.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private val gson = Gson()
    
    private val _serverSettings = MutableStateFlow(loadSettings())
    val serverSettings: Flow<ServerSettings> = _serverSettings.asStateFlow()
    
    private val _loginToken = MutableStateFlow<String?>(loadLoginToken())
    val loginToken: Flow<String?> = _loginToken.asStateFlow()
    
    private val _boardTokens = MutableStateFlow<Map<String, String>>(loadBoardTokens())
    val boardTokens: Flow<Map<String, String>> = _boardTokens.asStateFlow()
    
    private fun loadSettings(): ServerSettings {
        val serverIp = prefs.getString("server_ip", "") ?: ""
        val lecturerUsername = prefs.getString("lecturer_username", "lecturer1") ?: "lecturer1"
        val lecturerPassword = prefs.getString("lecturer_password", "lecturer123") ?: "lecturer123"
        val boardUsernames = prefs.getString("board_usernames", "board1, board2, board3") ?: "board1, board2, board3"
        val refreshInterval = prefs.getLong("refresh_interval", 10000L)
        
        // Load individual board credentials
        val boardCredentialsJson = prefs.getString("board_credentials", "{}")
        val boardCredentials = try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(boardCredentialsJson, type) ?: emptyMap<String, String>()
        } catch (e: Exception) {
            emptyMap<String, String>()
        }
        
        return ServerSettings(
            serverIp = serverIp,
            lecturerUsername = lecturerUsername,
            lecturerPassword = lecturerPassword,
            boardUsernames = boardUsernames,
            boardCredentials = boardCredentials,
            refreshInterval = refreshInterval
        )
    }
    
    private fun loadLoginToken(): String? {
        return prefs.getString("login_token", null)
    }
    
    private fun loadBoardTokens(): Map<String, String> {
        val boardTokensJson = prefs.getString("board_tokens", "{}")
        return try {
            val gson = Gson()
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(boardTokensJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun saveSettings(settings: ServerSettings) {
        prefs.edit().apply {
            putString("server_ip", settings.serverIp)
            putString("lecturer_username", settings.lecturerUsername)
            putString("lecturer_password", settings.lecturerPassword)
            putString("board_usernames", settings.boardUsernames)
            putLong("refresh_interval", settings.refreshInterval)
            
            // Save individual board credentials
            val gson = Gson()
            putString("board_credentials", gson.toJson(settings.boardCredentials))
            
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
    
    suspend fun loginBoards(): Result<Map<String, String>> {
        val settings = _serverSettings.value
        val boardUsernamesList = settings.boardUsernames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        return try {
            val results = mutableListOf<Pair<String, LoginResponse?>>()
            
            // Login each board with its individual password or default password
            for (boardUsername in boardUsernamesList) {
                val boardPassword = settings.boardCredentials[boardUsername] ?: "board123"
                try {
                    val loginResult = apiService.login(settings.serverIp, boardUsername, boardPassword)
                    if (loginResult.isSuccess) {
                        val loginResponse = loginResult.getOrNull()
                        // After login, register the board
                        if (loginResponse != null) {
                            val registerResult = apiService.registerBoard(settings.serverIp, loginResponse.token)
                            if (registerResult.isSuccess) {
                                results.add(boardUsername to loginResponse)
                            } else {
                                results.add(boardUsername to null)
                            }
                        } else {
                            results.add(boardUsername to null)
                        }
                    } else {
                        results.add(boardUsername to null)
                    }
                } catch (e: Exception) {
                    results.add(boardUsername to null)
                }
            }
            
            val successfulTokens = mutableMapOf<String, String>()
            for ((boardUsername, loginResponse) in results) {
                loginResponse?.token?.let { token ->
                    successfulTokens[boardUsername] = token
                }
            }
            
            // Update the board tokens
            _boardTokens.value = successfulTokens
            
            // Persist the board tokens
            val gson = Gson()
            prefs.edit().putString("board_tokens", gson.toJson(successfulTokens)).apply()
            
            Result.success(successfulTokens)
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
        _boardTokens.value = emptyMap()
        // Remove tokens from SharedPreferences
        prefs.edit().remove("login_token").remove("board_tokens").apply()
    }
    
    fun isLoggedIn(): Boolean {
        return _loginToken.value != null
    }
    
    fun areBoardsLoggedIn(): Boolean {
        return _boardTokens.value.isNotEmpty()
    }
    
    fun getLoggedInBoardCount(): Int {
        return _boardTokens.value.size
    }

    fun getCurrentServerSettings(): ServerSettings {
        return _serverSettings.value
    }

    suspend fun submitBoardData(
        boardId: String,
        production: Int,
        consumption: Int,
        powerGenerationByType: Map<String, Double>? = null
    ): Result<eu.swpelc.boardsimulator.network.BoardSubmitResponse> {
        val settings = _serverSettings.value
        val token = _loginToken.value
        
        return if (token != null) {
            apiService.submitBoardData(
                serverIp = settings.serverIp,
                token = token,
                boardId = boardId,
                production = production,
                consumption = consumption,
                powerGenerationByType = powerGenerationByType
            )
        } else {
            Result.failure(Exception("Not logged in as lecturer"))
        }
    }

    suspend fun nextRound(): eu.swpelc.boardsimulator.network.ApiResponse {
        val settings = _serverSettings.value
        val token = _loginToken.value
        
        return if (token != null) {
            apiService.nextRound(settings.serverIp, token)
        } else {
            throw Exception("Not logged in as lecturer")
        }
    }

    suspend fun getStatistics(): eu.swpelc.boardsimulator.network.ApiResponse {
        val settings = _serverSettings.value
        val token = _loginToken.value
        
        // Log.d("SettingsRepository", "getStatistics - Server: ${settings.serverIp}, Token: ${token?.take(10)}...")
        
        return if (token != null) {
            // Use simulation_dump but convert Result to ApiResponse for compatibility
            try {
                val result = apiService.getSimulationDump(settings.serverIp, token)
                if (result.isSuccess) {
                    val simulationDump = result.getOrNull()
                    val jsonBody = gson.toJson(simulationDump)
                    eu.swpelc.boardsimulator.network.ApiResponse(
                        isSuccessful = true,
                        code = 200,
                        body = jsonBody,
                        errorBody = null
                    )
                } else {
                    val exception = result.exceptionOrNull()
                    eu.swpelc.boardsimulator.network.ApiResponse(
                        isSuccessful = false,
                        code = -1,
                        body = null,
                        errorBody = exception?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                eu.swpelc.boardsimulator.network.ApiResponse(
                    isSuccessful = false,
                    code = -1,
                    body = null,
                    errorBody = e.message ?: "Unknown error"
                )
            }
        } else {
            throw Exception("Not logged in as lecturer")
        }
    }
}
