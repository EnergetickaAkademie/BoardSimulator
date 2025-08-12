package eu.swpelc.boardsimulator.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import eu.swpelc.boardsimulator.databinding.FragmentGameBinding
import eu.swpelc.boardsimulator.repository.SettingsRepository
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class GameFragment : Fragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsRepository: SettingsRepository
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        settingsRepository = SettingsRepository(requireContext())
        
        setupClickListeners()
        refreshGameStatus()
    }

    private fun testLoginStatus() {
        lifecycleScope.launch {
            val isLoggedIn = settingsRepository.isLoggedIn()
            val serverSettings = settingsRepository.getCurrentServerSettings()
            
            Log.d("GameFragment", "Login status: $isLoggedIn")
            Log.d("GameFragment", "Server IP: ${serverSettings.serverIp}")
            Log.d("GameFragment", "Lecturer username: ${serverSettings.lecturerUsername}")
            
            val message = """
                Login Status: $isLoggedIn
                Server IP: ${serverSettings.serverIp}
                Lecturer: ${serverSettings.lecturerUsername}
                Boards logged in: ${settingsRepository.getLoggedInBoardCount()}
            """.trimIndent()
            
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.buttonNextRound.setOnClickListener {
            nextRound()
        }
        
        binding.buttonRefreshStatus.setOnClickListener {
            refreshGameStatus()
        }
        
        binding.buttonTestLogin.setOnClickListener {
            testLoginStatus()
        }
    }

    private fun nextRound() {
        lifecycleScope.launch {
            try {
                val response = settingsRepository.nextRound()
                if (response.isSuccessful) {
                    Toast.makeText(context, "Next round initiated", Toast.LENGTH_SHORT).show()
                    refreshGameStatus()
                } else {
                    Log.e("GameFragment", "Next round failed: ${response.code} - ${response.errorBody}")
                    Toast.makeText(context, "Failed to advance round: ${response.code}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("GameFragment", "Error during next round", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun refreshGameStatus() {
        lifecycleScope.launch {
            try {
                Log.d("GameFragment", "Refreshing game status...")
                
                // Check if we're logged in first
                if (!settingsRepository.isLoggedIn()) {
                    binding.textStatistics.text = "Not logged in as lecturer"
                    binding.textCurrentRound.text = "Current Round: Login Required"
                    binding.textRoundType.text = "Round Type: N/A"
                    binding.textGameActive.text = "Game Active: N/A"
                    return@launch
                }
                
                // Get statistics
                val statsResponse = settingsRepository.getStatistics()
                Log.d("GameFragment", "Statistics response - Success: ${statsResponse.isSuccessful}, Code: ${statsResponse.code}")
                
                if (statsResponse.isSuccessful) {
                    val statsBody = statsResponse.body
                    // Log.d("GameFragment", "Statistics response body: $statsBody") // Commented out to reduce logs
                    
                    if (statsBody != null && statsBody.isNotEmpty()) {
                        updateStatisticsDisplay(statsBody)
                        
                        // Try to parse game info from statistics
                        try {
                            val type = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                            val statsJson: Map<String, Any> = gson.fromJson(statsBody, type)
                            // Log.d("GameFragment", "Parsed statistics: $statsJson") // Commented out to reduce logs
                            updateGameStatusFromStats(statsJson)
                        } catch (e: Exception) {
                            Log.w("GameFragment", "Could not parse statistics as JSON", e)
                            binding.textStatistics.text = "Raw response: $statsBody"
                        }
                    } else {
                        Log.w("GameFragment", "Empty response body")
                        binding.textStatistics.text = "Empty response from server"
                    }
                } else {
                    Log.e("GameFragment", "Failed to get statistics: ${statsResponse.code} - ${statsResponse.errorBody}")
                    binding.textStatistics.text = "Failed to load statistics: ${statsResponse.code}\nError: ${statsResponse.errorBody}"
                    binding.textCurrentRound.text = "Current Round: API Error"
                    binding.textRoundType.text = "Round Type: Error"
                    binding.textGameActive.text = "Game Active: Error"
                }
            } catch (e: Exception) {
                Log.e("GameFragment", "Error refreshing game status", e)
                binding.textStatistics.text = "Error loading statistics: ${e.message}"
                binding.textCurrentRound.text = "Current Round: Exception"
                binding.textRoundType.text = "Round Type: Exception"
                binding.textGameActive.text = "Game Active: Exception"
            }
        }
    }

    private fun updateStatisticsDisplay(statsBody: String) {
        try {
            // Try to format as JSON for better display
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val jsonObject: Map<String, Any> = gson.fromJson(statsBody, type)
            val formattedStats = formatStatsForDisplay(jsonObject)
            binding.textStatistics.text = formattedStats
        } catch (e: Exception) {
            // If not valid JSON, display as-is
            binding.textStatistics.text = statsBody
        }
    }

    private fun formatStatsForDisplay(stats: Map<String, Any>): String {
        val sb = StringBuilder()
        
        // Get board statistics from groups data structure
        val groups = stats["groups"] as? Map<*, *>
        val firstGroup = groups?.values?.firstOrNull() as? Map<*, *>
        val boards = firstGroup?.get("boards") as? Map<*, *>
        
        if (boards != null) {
            boards.forEach { (_, board) ->
                if (board is Map<*, *>) {
                    val boardId = board["board_id"] ?: "Unknown"
                    val currentProd = (board["production"] as? Number)?.toInt() ?: 0
                    val currentCons = (board["consumption"] as? Number)?.toInt() ?: 0
                    
                    sb.append("Board $boardId: ${currentProd}MW / ${currentCons}MW\n")
                }
            }
        } else {
            // Fallback to old statistics structure if available
            val statistics = stats["statistics"]
            if (statistics is List<*>) {
                statistics.forEach { board ->
                    if (board is Map<*, *>) {
                        val boardId = board["board_id"] ?: "Unknown"
                        val currentProd = (board["current_production"] as? Number)?.toInt() ?: 0
                        val currentCons = (board["current_consumption"] as? Number)?.toInt() ?: 0
                        
                        sb.append("Board $boardId: ${currentProd}MW / ${currentCons}MW\n")
                    }
                }
            }
        }
        
        return sb.toString().trimEnd()
    }

    private fun updateGameStatusFromStats(stats: Map<String, Any>) {
        // Look for game status in groups structure first, then fallback to direct game_status
        val groups = stats["groups"] as? Map<*, *>
        val firstGroup = groups?.values?.firstOrNull() as? Map<*, *>
        val gameStatus = firstGroup?.get("game_status") ?: stats["game_status"]
        
        if (gameStatus is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val gameStatusMap = gameStatus as Map<String, Any>
            
            // Format as integers
            val currentRound = (gameStatusMap["current_round"] as? Number)?.toInt() ?: 0
            val totalRounds = (gameStatusMap["total_rounds"] as? Number)?.toInt() ?: 0
            val scenario = gameStatusMap["scenario"]?.toString() ?: "Unknown"
            val gameActive = gameStatusMap["game_active"]?.toString() ?: gameStatusMap["active"]?.toString() ?: "false"
            val roundType = (gameStatusMap["round_type"] as? Number)?.toInt() ?: 0
            
            // Convert round type enum to readable format
            val roundTypeName = when (roundType) {
                1 -> "DAY"
                2 -> "NIGHT" 
                3 -> "SLIDE"
                4 -> "SLIDE_RANGE"
                else -> "UNKNOWN ($roundType)"
            }
            
            binding.textCurrentRound.text = "Current Round: $currentRound/$totalRounds"
            binding.textRoundType.text = "Scenario: $scenario | Round Type: $roundTypeName"
            binding.textGameActive.text = "Game Active: ${if (gameActive == "true") "Yes" else "No"}"
            
            // Check for weather and slide info
            updateGameDetails(stats)
        }
    }

    private fun updateGameDetails(stats: Map<String, Any>) {
        var hasDetails = false
        
        // Check if we have groups data (from simulation_dump) which contains more info
        val groups = stats["groups"]
        if (groups is Map<*, *> && groups.isNotEmpty()) {
            showGameDetails(stats)
            hasDetails = true
        } else {
            // Check for weather data (from get_statistics)
            val weather = stats["weather"] ?: stats["weather_data"]
            if (weather != null) {
                showWeatherDetails(weather)
                hasDetails = true
            } else {
                hideWeatherDetails()
            }
        }
        
        // Check for slide path/info
        val slideInfo = stats["slide_info"] ?: stats["slide_path"]
        if (slideInfo != null) {
            showSlideDetails(slideInfo)
            hasDetails = true
        } else {
            hideSlideDetails()
        }
        
        // Show the card if we have any details
        binding.cardRoundDetails.visibility = if (hasDetails) View.VISIBLE else View.GONE
    }

    private fun showWeatherDetails(weather: Any) {
        binding.layoutGameDetails.visibility = View.VISIBLE
        
        if (weather is Map<*, *>) {
            val weatherText = weather.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            binding.textWeatherInfo.text = "Weather: $weatherText"
        } else {
            binding.textWeatherInfo.text = "Weather: $weather"
        }
        
        // Clear these since weather display doesn't have this data
        binding.textProductionCoefficients.text = "Not available with weather data"
        binding.textConsumptionModifiers.text = "Not available with weather data"
    }

    private fun showGameDetails(stats: Map<String, Any>) {
        binding.layoutGameDetails.visibility = View.VISIBLE
        
        // Weather info (if available)
        val weather = stats["weather"] ?: stats["weather_data"]
        if (weather != null) {
            if (weather is Map<*, *>) {
                val weatherText = weather.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                binding.textWeatherInfo.text = "Weather: $weatherText"
            } else {
                binding.textWeatherInfo.text = "Weather: $weather"
            }
        } else {
            binding.textWeatherInfo.text = "Weather: Not available"
        }
        
        // Production coefficients (look in groups data structure)
        val groups = stats["groups"] as? Map<*, *>
        val firstGroup = groups?.values?.firstOrNull() as? Map<*, *>
        
        val prodCoeff = firstGroup?.get("production_coefficients") ?: stats["production_coefficients"]
        if (prodCoeff is Map<*, *>) {
            val coeffText = prodCoeff.entries.joinToString("\n") { 
                val percentage = if (it.value is Number) {
                    String.format("%.1f%%", (it.value as Number).toDouble() * 100)
                } else it.value.toString()
                "${it.key}: $percentage"
            }
            binding.textProductionCoefficients.text = coeffText
        } else {
            binding.textProductionCoefficients.text = "Not available"
        }
        
        // Powerplant ranges instead of consumption modifiers for game details
        val powerplantRanges = firstGroup?.get("powerplant_ranges")
        if (powerplantRanges is Map<*, *>) {
            val rangesText = powerplantRanges.entries.joinToString("\n") { entry ->
                val plantName = entry.key
                val ranges = entry.value as? Map<*, *>
                val min = ranges?.get("min") as? Number
                val max = ranges?.get("max") as? Number
                if (min != null && max != null) {
                    "$plantName: ${min.toInt()}-${max.toInt()}MW"
                } else {
                    "$plantName: Invalid range"
                }
            }
            binding.textConsumptionModifiers.text = rangesText
        } else {
            binding.textConsumptionModifiers.text = "Not available"
        }
    }

    private fun showSlideDetails(slideInfo: Any) {
        binding.layoutSlideDetails.visibility = View.VISIBLE
        
        if (slideInfo is Map<*, *>) {
            val slideText = slideInfo.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            binding.textSlideInfo.text = "Slide Information: $slideText"
        } else {
            binding.textSlideInfo.text = "Slide Path: $slideInfo"
        }
    }

    private fun hideWeatherDetails() {
        binding.layoutGameDetails.visibility = View.GONE
    }

    private fun hideSlideDetails() {
        binding.layoutSlideDetails.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
