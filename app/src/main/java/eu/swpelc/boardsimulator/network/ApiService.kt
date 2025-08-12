package eu.swpelc.boardsimulator.network

import eu.swpelc.boardsimulator.model.LoginResponse
import eu.swpelc.boardsimulator.model.SimulationDump
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

class ApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
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
}
