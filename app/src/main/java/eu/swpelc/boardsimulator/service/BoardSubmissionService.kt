package eu.swpelc.boardsimulator.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel

class BoardSubmissionService private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: BoardSubmissionService? = null
        
        fun getInstance(context: Context): BoardSubmissionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BoardSubmissionService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val settingsRepository = SettingsRepository(context)
    private val boardConfigRepository = BoardConfigRepository.getInstance(context)
    private var submissionJob: Job? = null
    private var currentInterval = 10000L
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun startPeriodicSubmission() {
        stopPeriodicSubmission()
        
        submissionJob = scope.launch {
            while (isActive) {
                try {
                    delay(currentInterval)
                    if (isActive) {
                        submitAllBoardData()
                    }
                } catch (e: Exception) {
                    Log.e("BoardSubmissionService", "Error in periodic submission", e)
                }
            }
        }
        Log.d("BoardSubmissionService", "Started global board data submission, interval: $currentInterval ms.")
    }
    
    fun stopPeriodicSubmission() {
        submissionJob?.cancel()
        submissionJob = null
        Log.d("BoardSubmissionService", "Stopped global board data submission.")
    }
    
    fun updateInterval(newInterval: Long) {
        if (currentInterval != newInterval) {
            currentInterval = newInterval
            Log.d("BoardSubmissionService", "Board submission interval changed to: $currentInterval ms.")
            if (submissionJob?.isActive == true) {
                startPeriodicSubmission() // Restart with new interval
            }
        }
    }
    
    private suspend fun submitAllBoardData() {
        try {
            // Get current settings
            val settings = settingsRepository.getCurrentServerSettings()
            val boardUsernames = settings.boardUsernames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            if (boardUsernames.isEmpty()) {
                Log.d("BoardSubmissionService", "No board usernames configured, skipping submission.")
                return
            }
            
            // Check if lecturer is logged in
            val lecturerToken = settingsRepository.getCurrentLoginToken()
            if (lecturerToken.isNullOrEmpty()) {
                Log.d("BoardSubmissionService", "Not logged in as lecturer, skipping submission.")
                return
            }
            
            // Get simulation dump
            val simulationDumpResult = settingsRepository.apiService.getSimulationDump(settings.serverIp, lecturerToken)
            if (simulationDumpResult.isFailure) {
                Log.d("BoardSubmissionService", "Failed to get simulation data: ${simulationDumpResult.exceptionOrNull()?.message}")
                return
            }
            
            val dump = simulationDumpResult.getOrNull()
            if (dump == null) {
                Log.d("BoardSubmissionService", "No simulation data available, skipping.")
                return
            }
            
            val groupData = dump.groups.values.firstOrNull()
            if (groupData == null) {
                Log.d("BoardSubmissionService", "No group data available, skipping.")
                return
            }
            
            val productionCoefficients = groupData.production_coefficients ?: emptyMap()
            val powerplantRanges = groupData.powerplant_ranges ?: emptyMap()
            val consumptionModifiers = groupData.consumption_modifiers ?: emptyMap()
            
            Log.d("BoardSubmissionService", "Production coefficients: $productionCoefficients")
            Log.d("BoardSubmissionService", "Powerplant ranges: $powerplantRanges")
            
            Log.d("BoardSubmissionService", "Submitting data for ${boardUsernames.size} boards")
            
            // Submit data for each board
            for (boardId in boardUsernames) {
                submitSingleBoardData(boardId, productionCoefficients, powerplantRanges, consumptionModifiers, settings, lecturerToken)
            }
            
        } catch (e: Exception) {
            Log.e("BoardSubmissionService", "Error in submitAllBoardData", e)
        }
    }
    
    private suspend fun submitSingleBoardData(
        boardId: String,
        productionCoefficients: Map<String, Double>,
        powerplantRanges: Map<String, *>,
        consumptionModifiers: Map<String, Double>,
        settings: eu.swpelc.boardsimulator.model.ServerSettings,
        lecturerToken: String
    ) {
        try {
            val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
            
            Log.d("BoardSubmissionService", "Board $boardId config: powerplants=${boardConfig.powerplants}, productionLevels=${boardConfig.productionLevels}")
            
            var totalProduction = 0.0
            var totalConsumption = 0.0
            val powerGenerationByType = mutableMapOf<String, Double>()
            
            // Calculate production using saved production levels directly
            boardConfig.powerplants.forEach { (powerplantName, quantity) ->
                if (quantity > 0) {
                    val savedProductionLevel = boardConfigRepository.getProductionLevel(boardId, powerplantName)
                    
                    if (savedProductionLevel != null && savedProductionLevel > 0) {
                        // Use the saved production level directly
                        totalProduction += savedProductionLevel
                        val powerplantKey = powerplantName.uppercase()
                        powerGenerationByType[powerplantKey] = (powerGenerationByType[powerplantKey] ?: 0.0) + savedProductionLevel
                        Log.d("BoardSubmissionService", "Board $boardId: $powerplantName x$quantity -> savedLevel=$savedProductionLevel (using directly)")
                    } else {
                        Log.d("BoardSubmissionService", "Board $boardId: $powerplantName x$quantity -> no saved production level or level is 0, skipping")
                    }
                }
            }
            
            // Calculate consumption
            boardConfig.buildings.forEach { (buildingName, quantity) ->
                if (quantity > 0) {
                    val consumption = consumptionModifiers[buildingName] 
                        ?: consumptionModifiers[buildingName.uppercase().replace(" ", "_")] 
                        ?: 0.0 
                    totalConsumption += consumption * quantity
                }
            }
            
            val productionInt = totalProduction.toInt()
            val consumptionInt = totalConsumption.toInt()
            
            // Submit to server
            val result = settingsRepository.apiService.submitBoardData(
                serverIp = settings.serverIp,
                token = lecturerToken,
                boardId = boardId,
                production = productionInt,
                consumption = consumptionInt,
                powerGenerationByType = powerGenerationByType.mapValues { it.value }
            )
            
            if (result.isSuccess) {
                Log.d("BoardSubmissionService", "Successfully submitted board $boardId: Production=$productionInt MW, Consumption=$consumptionInt MW")
            } else {
                Log.w("BoardSubmissionService", "Failed to submit board $boardId: ${result.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e("BoardSubmissionService", "Error submitting board $boardId", e)
        }
    }
    
    fun cleanup() {
        stopPeriodicSubmission()
        scope.cancel()
    }
}
