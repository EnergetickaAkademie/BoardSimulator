package eu.swpelc.boardsimulator.network

import android.util.Log
import eu.swpelc.boardsimulator.model.LoginResponse
import eu.swpelc.boardsimulator.model.SimulationDump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val gson = Gson()
    
    suspend fun login(serverIp: String, username: String, password: String): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val json = """{"username": "$username", "password": "$password"}"""
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    // Add http:// prefix if not present
                    // If no port is specified, don't add :80 (let HTTP default handle it)
                    "http://$serverIp"
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/login")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                        Result.success(loginResponse)
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    Result.failure(Exception("Login failed: ${response.code} ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun loginMultipleBoards(serverIp: String, boardUsernames: List<String>, password: String = "board123"): Result<List<Pair<String, LoginResponse?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<Pair<String, LoginResponse?>>()
                
                for (boardUsername in boardUsernames) {
                    try {
                        val loginResult = login(serverIp, boardUsername, password)
                        if (loginResult.isSuccess) {
                            results.add(boardUsername to loginResult.getOrNull())
                        } else {
                            results.add(boardUsername to null)
                        }
                    } catch (e: Exception) {
                        results.add(boardUsername to null)
                    }
                }
                
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun registerBoard(serverIp: String, token: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    "http://$serverIp"
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/register")
                    .addHeader("Authorization", "Bearer $token")
                    .post("".toRequestBody("application/octet-stream".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Board registration failed: ${response.code} ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun loginAndRegisterBoards(serverIp: String, boardUsernames: List<String>, password: String = "board123"): Result<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val successfulTokens = mutableMapOf<String, String>()
                
                for (boardUsername in boardUsernames) {
                    try {
                        // First login
                        val loginResult = login(serverIp, boardUsername, password)
                        if (loginResult.isSuccess) {
                            val token = loginResult.getOrNull()?.token
                            if (token != null) {
                                // Then register
                                val registerResult = registerBoard(serverIp, token)
                                if (registerResult.isSuccess) {
                                    successfulTokens[boardUsername] = token
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Continue with next board if one fails
                    }
                }
                
                Result.success(successfulTokens)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getSimulationDump(serverIp: String, token: String): Result<SimulationDump> {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    // Add http:// prefix if not present
                    // If no port is specified, don't add :80 (let HTTP default handle it)
                    "http://$serverIp"
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/lecturer/simulation_dump")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val simulationDump = gson.fromJson(responseBody, SimulationDump::class.java)
                        Result.success(simulationDump)
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    Result.failure(Exception("Failed to get simulation dump: ${response.code} ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun submitBoardData(
        serverIp: String, 
        token: String, 
        boardId: String, 
        production: Int, 
        consumption: Int,
        powerGenerationByType: Map<String, Double>? = null,
        connectedProduction: List<Int>? = null,
        connectedConsumption: List<Int>? = null
    ): Result<BoardSubmitResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val submitData = BoardSubmitRequest(
                    board_id = boardId,
                    production = production,
                    consumption = consumption,
                    power_generation_by_type = powerGenerationByType,
                    connected_production = connectedProduction,
                    connected_consumption = connectedConsumption
                )
                
                val json = gson.toJson(submitData)
                val requestBody = json.toRequestBody("application/json".toMediaType())
                
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    "http://$serverIp"
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/lecturer/submit_board_data")
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val submitResponse = gson.fromJson(responseBody, BoardSubmitResponse::class.java)
                        Result.success(submitResponse)
                    } else {
                        Result.failure(Exception("Empty response body"))
                    }
                } else {
                    val errorBody = response.body?.string()
                    Result.failure(Exception("Failed to submit board data: ${response.code} ${response.message} - $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun nextRound(serverIp: String, token: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    "http://$serverIp"
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/next_round")
                    .addHeader("Authorization", "Bearer $token")
                    .post("".toRequestBody("application/json".toMediaType()))
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                val isSuccessful = response.isSuccessful
                val code = response.code
                response.close() // Close the response to prevent leaks
                
                ApiResponse(
                    isSuccessful = isSuccessful,
                    code = code,
                    body = responseBody,
                    errorBody = if (!isSuccessful) responseBody else null
                )
            } catch (e: Exception) {
                ApiResponse(
                    isSuccessful = false,
                    code = -1,
                    body = null,
                    errorBody = e.message
                )
            }
        }
    }

    suspend fun getStatistics(serverIp: String, token: String): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (serverIp.startsWith("http://") || serverIp.startsWith("https://")) {
                    serverIp
                } else {
                    "http://$serverIp"
                }
                
                // Log.d("ApiService", "Getting statistics from: $baseUrl/coreapi/get_statistics")
                
                val request = Request.Builder()
                    .url("$baseUrl/coreapi/get_statistics")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                response.close() // Close the response to prevent leaks
                
                // Log.d("ApiService", "Statistics response - Code: ${response.code}, Success: ${response.isSuccessful}")
                // Log.d("ApiService", "Statistics response body: $responseBody") // Commented out to reduce logs
                
                ApiResponse(
                    isSuccessful = response.isSuccessful,
                    code = response.code,
                    body = responseBody,
                    errorBody = if (!response.isSuccessful) responseBody else null
                )
            } catch (e: Exception) {
                Log.e("ApiService", "Exception getting statistics", e)
                ApiResponse(
                    isSuccessful = false,
                    code = -1,
                    body = null,
                    errorBody = "Connection error: ${e.message}"
                )
            }
        }
    }
}

data class ApiResponse(
    val isSuccessful: Boolean,
    val code: Int,
    val body: String?,
    val errorBody: String?
)

data class BoardSubmitRequest(
    val board_id: String,
    val production: Int,
    val consumption: Int,
    val power_generation_by_type: Map<String, Double>? = null,
    val connected_production: List<Int>? = null,
    val connected_consumption: List<Int>? = null
)

data class BoardSubmitResponse(
    val success: Boolean,
    val message: String,
    val spoofed_by: String? = null,
    val data: BoardSubmitData? = null
)

data class BoardSubmitData(
    val group_id: String,
    val board_id: String,
    val production: Int,
    val consumption: Int,
    val timestamp: Double,
    val power_generation_by_type: Map<String, Double>? = null
)
