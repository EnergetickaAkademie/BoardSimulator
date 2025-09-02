package eu.swpelc.boardsimulator.ui.boarddetail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import eu.swpelc.boardsimulator.databinding.ActivityBoardDetailBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.building.BuildingSelectionActivity
import eu.swpelc.boardsimulator.ui.building.BuildingListActivity
import eu.swpelc.boardsimulator.ui.powerplant.PowerplantSelectionActivity
import eu.swpelc.boardsimulator.ui.powerplant.PowerplantListActivity
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import eu.swpelc.boardsimulator.service.BoardSubmissionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class BoardDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityBoardDetailBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var boardSubmissionService: BoardSubmissionService
    private var boardId: String = ""
    private var periodicRefreshJob: Job? = null
    private var lastBoardData: eu.swpelc.boardsimulator.model.BoardData? = null
    private var lastLocalProduction: Double? = null
    private var lastLocalConsumption: Double? = null
    private var lastLocalMaxProduction: Double? = null
    private var isPeriodicRefresh = false // For server data refresh indication
    private var currentRefreshInterval = 10000L // For server data

    private var localTotalsPeriodicRefreshJob: Job? = null
    private val localTotalsRefreshIntervalMillis = 1000L // Refresh local totals every 1 second

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBoardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        boardId = intent.getStringExtra(EXTRA_BOARD_ID) ?: ""
        boardConfigRepository = BoardConfigRepository.getInstance(this)
        boardSubmissionService = BoardSubmissionService.getInstance(this)
        
        setupActionBar()
        setupViewModel()
        setupObservers()
        setupClickListeners()
        
        // Fetch initial simulation dump on startup
        refreshData() // Fetches server data
    }

    override fun onResume() {
        super.onResume()
        refreshData()
        startPeriodicRefresh()
        // Add this line to force recalculation
        forceUpdateLocalConfiguration()
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        stopPeriodicRefresh() // Stop any existing server data refresh job
        periodicRefreshJob = lifecycleScope.launch {
            while (isActive) { // Use isActive from the coroutine scope
                delay(currentRefreshInterval)
                if(isActive) {
                    // Log.d("BoardDetailActivity", "Server data periodic refresh: Fetching new simulation dump.")
                    isPeriodicRefresh = true // Flag for UI loading indicator
                    refreshData() // Fetches server data
                    isPeriodicRefresh = false
                }
            }
        }
        Log.d("BoardDetailActivity", "Started server data periodic refresh, interval: $currentRefreshInterval ms.")
    }
    
    private fun stopPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        isPeriodicRefresh = false // Reset flag
        Log.d("BoardDetailActivity", "Server data periodic refresh stopped.")
    }

    private fun refreshData() { // Renamed for clarity: this is for server data
        settingsViewModel.fetchSimulationDump()
    }

    private fun refreshLocalTotals() {
        // This function is now called by the local totals periodic refresh timer
        // and also after specific actions like reset or manual refresh button.

        // Reset last known values to force UI update attempt.
        lastLocalProduction = null
        lastLocalConsumption = null
        lastLocalMaxProduction = null

        val dump = settingsViewModel.simulationDump.value
        // Log.d("BoardDetail", "Simulation dump is null for local totals: ${dump == null}")
        if (dump != null) {
            updateLocalConfigurationTotals(dump)
        } else {
            Log.d("BoardDetail", "Dump is null, cannot update local totals for now.")
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "$boardId Configuration"
        }
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.simulationDump.collect { dump ->
                dump?.let {
                    val boardData = dump.groups.values.firstOrNull()?.boards?.get(boardId)
                    boardData?.let { board ->
                        updateBoardInfo(board)
                    }
                    // This will also be triggered by the periodic server refresh via fetchSimulationDump()
                    // And by manual refresh button
                    // Log.d("BoardDetailActivity", "SimulationDump collected. Refreshing local totals.")
                    refreshLocalTotals() 
                }
            }
        }

        lifecycleScope.launch {
            boardConfigRepository.getCombinedBoardConfigurationFlow(boardId).collect { config ->
                // Log.d("BoardDetailActivity", "Combined board configuration changed. Refreshing local totals.")
                refreshLocalTotals()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                val shouldShowLoading = isLoading && !isPeriodicRefresh 
                binding.progressBar.visibility = if (shouldShowLoading) android.view.View.VISIBLE else android.view.View.INVISIBLE
            }
        }

        lifecycleScope.launch {
            settingsViewModel.serverSettings.collect { settings ->
                if (currentRefreshInterval != settings.refreshInterval) {
                    currentRefreshInterval = settings.refreshInterval
                    Log.d("BoardDetailActivity", "Server refresh interval changed to: $currentRefreshInterval. Restarting server refresh.")
                    startPeriodicRefresh() // Restart server periodic refresh with new interval
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.errorMessage.collect { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(this@BoardDetailActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonAdjustProduction.setOnClickListener {
            // isPeriodicRefresh = false // Handled by onPause of this activity
            // refreshData() // Let other activity handle its data needs
            val intent = Intent(this, eu.swpelc.boardsimulator.ui.production.ProductionAdjustmentActivity::class.java)
            intent.putExtra(eu.swpelc.boardsimulator.ui.production.ProductionAdjustmentActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonAddBuildings.setOnClickListener {
            val intent = Intent(this, BuildingSelectionActivity::class.java)
            intent.putExtra(BuildingSelectionActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonAddPowerplants.setOnClickListener {
            val intent = Intent(this, PowerplantSelectionActivity::class.java)
            intent.putExtra(PowerplantSelectionActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonViewBuildings.setOnClickListener {
            val intent = Intent(this, BuildingListActivity::class.java)
            intent.putExtra(BuildingListActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonViewPowerplants.setOnClickListener {
            val intent = Intent(this, PowerplantListActivity::class.java)
            intent.putExtra(PowerplantListActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonResetBoard.setOnClickListener {
            showResetConfirmationDialog()
        }
        
        binding.buttonRefresh?.setOnClickListener {
            // isPeriodicRefresh = false // Handled by stopPeriodicRefresh if active
            Log.d("BoardDetailActivity", "Manual Refresh button clicked.")

            Log.d("BoardDetailActivity", "Performing immediate local totals refresh with current dump and board config.")
            refreshLocalTotals() // Recalculate and show local totals immediately

            Log.d("BoardDetailActivity", "Initiating fetch of new simulation dump from server.")
            settingsViewModel.fetchSimulationDump() // Fetch server data
            
            Toast.makeText(this, "Refreshing board data and local totals...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Board")
            .setMessage("Are you sure you want to reset all buildings and powerplants for $boardId? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                resetBoard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetBoard() {
        boardConfigRepository.resetBoard(boardId)
        Toast.makeText(this, "Board $boardId has been reset", Toast.LENGTH_SHORT).show()
        
        // Refresh the simulation dump and local totals to update the display
        settingsViewModel.fetchSimulationDump() // To update server view if necessary
        refreshLocalTotals() // To update local totals view immediately
    }

    private fun updateBoardInfo(boardData: eu.swpelc.boardsimulator.model.BoardData) {
        if (lastBoardData?.let { 
            it.production == boardData.production &&
            it.consumption == boardData.consumption &&
            it.power_generation_by_type == boardData.power_generation_by_type // Check this too
        } == true) {
            // Log.d("BoardDetailActivity", "Board info (server data) unchanged, skipping UI update.")
            return
        }
        
        lastBoardData = boardData
        // Log.d("BoardDetailActivity", "Updating board info UI with new server data.")
        
        val production = boardData.production.toInt()
        val consumption = boardData.consumption.toInt()
        val balance = production - consumption
        
        binding.textCurrentProduction.text = String.format("Current Production: %4d MW", production)
        binding.textCurrentConsumption.text = String.format("Current Consumption: %4d MW", consumption)
        binding.textBalance.text = String.format("Balance: %+5d MW", balance)
        
        val isActive = boardData.production > 0 || boardData.consumption > 0
        binding.textStatus.text = "Status: ${if (isActive) "Active  " else "Inactive"}"
        
        val powerGenByType = boardData.power_generation_by_type
        if (powerGenByType.isNotEmpty()) {
            binding.textPowerGenerationTitle.visibility = android.view.View.VISIBLE
            binding.textPowerGeneration.visibility = android.view.View.VISIBLE
            
            val powerGenText = powerGenByType.entries
                .sortedBy { it.key }
                .joinToString("\n") { (type, value) ->
                    String.format("  %s: %4.1f MW", type, value)
                }
            binding.textPowerGeneration.text = powerGenText
        } else {
            binding.textPowerGenerationTitle.visibility = android.view.View.GONE
            binding.textPowerGeneration.visibility = android.view.View.GONE
        }
    }

    private fun forceUpdateLocalConfiguration() {
        lastLocalProduction = null
        lastLocalConsumption = null
        lastLocalMaxProduction = null
        settingsViewModel.simulationDump.value?.let {
            updateLocalConfigurationTotals(it)
        }
    }

    private fun updateLocalConfigurationTotals(dump: eu.swpelc.boardsimulator.model.SimulationDump) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val groupData = dump.groups.values.firstOrNull()
        
        var totalCurrentProduction = 0.0
        var totalMaxProduction = 0.0
        val productionCoefficients = groupData?.production_coefficients ?: emptyMap()
        val powerplantRanges = groupData?.powerplant_ranges ?: emptyMap()
        
        boardConfig.powerplants.forEach { (powerplantName, quantity) ->
            if (quantity > 0) {
                val coefficient = productionCoefficients[powerplantName] ?: 0.0
                if (coefficient > 0) {
                    val range = powerplantRanges[powerplantName] ?: powerplantRanges[powerplantName.uppercase()]
                    val maxOutput = range?.max ?: 1.0
                    val minOutput = range?.min ?: 0.0
                    
                    val maxTheoreticalProductionForOne = maxOutput * coefficient
                    val maxProductionForQuantity = maxTheoreticalProductionForOne * quantity
                    totalMaxProduction += maxProductionForQuantity
                    
                    val savedProductionLevel = boardConfigRepository.getProductionLevel(boardId, powerplantName)
                    // If savedProductionLevel is null, it means it's not set for this powerplant,
                    // default to its max possible output for this quantity
                    val currentProduction = savedProductionLevel ?: maxProductionForQuantity 
                    
                    val minProductionForQuantity = (minOutput * coefficient) * quantity
                    val actualProduction = currentProduction.coerceIn(minProductionForQuantity, maxProductionForQuantity)
                    totalCurrentProduction += actualProduction
                }
            }
        }
        
        var totalConsumption = 0.0
        val consumptionModifiers = groupData?.consumption_modifiers ?: emptyMap()
        
        boardConfig.buildings.forEach { (buildingName, quantity) ->
            if (quantity > 0) {
                // Calculate consumption for this building
                var consumption = 0.0
                for (nameVariation in listOf(buildingName, buildingName.uppercase(), buildingName.lowercase())) {
                    val consumptionValue = consumptionModifiers[nameVariation]
                    if (consumptionValue != null) {
                        consumption = consumptionValue
                        // Log.d("BoardDetail", "Building: $buildingName, Qty: $quantity, Found consumption: $consumption MW per building")
                        break 
                    }
                }
                if (consumption == 0.0) {
                    val cleanBuildingName = buildingName.uppercase().replace(Regex("[^A-Z0-9]"), "")
                    for ((modifierKey, modifierValue) in consumptionModifiers) {
                        val cleanModifierKey = modifierKey.replace(Regex("[^A-Z0-9]"), "")
                        if (cleanModifierKey.contains(cleanBuildingName) || cleanBuildingName.contains(cleanModifierKey)) {
                            consumption = modifierValue
                            // Log.d("BoardDetail", "Building: $buildingName, Qty: $quantity, Found consumption via pattern matching: $consumption MW per building")
                            break
                        }
                    }
                }
                totalConsumption += consumption * quantity
                // Log.d("BoardDetail", "Building: $buildingName, Qty: $quantity, Individual consumption: $consumption MW, Total added: ${consumption * quantity} MW")
            }
        }
        
        // The KSP error pointed to line 318, which was inside the above commented block.
        // We also ensure the early return based on unchanged values is commented out, as per previous debugging steps.
        // if (lastLocalProduction == totalCurrentProduction && 
        //     lastLocalConsumption == totalConsumption &&
        //     lastLocalMaxProduction == totalMaxProduction) {
        //     Log.d("BoardDetail", "Local totals (including max) unchanged, skipping UI update.")
        //     return
        // }
        
        lastLocalProduction = totalCurrentProduction
        lastLocalConsumption = totalConsumption
        lastLocalMaxProduction = totalMaxProduction
        
        val currentBalance = totalCurrentProduction - totalConsumption
        
        // Log.d("BoardDetail", "Values changed - updating UI: Production=$totalCurrentProduction, Consumption=$totalConsumption, MaxProduction=$totalMaxProduction")
        
        // FORCE UI updates on UI thread with aggressive logging
        runOnUiThread {
            
            val productionText = String.format("Current Production: %6.1f MW (Max: %.1f)", totalCurrentProduction, totalMaxProduction)
            val consumptionText = String.format("Total Consumption: %6.1f MW", totalConsumption)
            val balanceText = String.format("Current Balance: %+6.1f MW", currentBalance)
            
            // Set the text values
            binding.textLocalMaxProduction.text = productionText
            binding.textLocalConsumption.text = consumptionText
            binding.textLocalBalance.text = balanceText
            
            // Force invalidate and request layout
            binding.textLocalMaxProduction.invalidate()
            binding.textLocalConsumption.invalidate()
            binding.textLocalBalance.invalidate()
            
            binding.textLocalMaxProduction.requestLayout()
            binding.textLocalConsumption.requestLayout()
            binding.textLocalBalance.requestLayout()
        }

    }

    private fun submitBoardDataToServer() {
        val dump = settingsViewModel.simulationDump.value
        if (dump == null) {
            Toast.makeText(this, "No simulation data available. Please refresh first.", Toast.LENGTH_LONG).show()
            return
        }

        if (!settingsViewModel.isLoggedIn()) {
            Toast.makeText(this, "Not logged in as lecturer. Please login first.", Toast.LENGTH_LONG).show()
            return
        }

        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val groupData = dump.groups.values.firstOrNull()
        
        if (groupData == null) {
            Toast.makeText(this, "No group data available.", Toast.LENGTH_LONG).show()
            return
        }

        var totalProduction = 0.0
        var totalConsumption = 0.0
        val powerGenerationByType = mutableMapOf<String, Double>()
        
        val productionCoefficients = groupData.production_coefficients ?: emptyMap()
        val powerplantRanges = groupData.powerplant_ranges ?: emptyMap()
        val consumptionModifiers = groupData.consumption_modifiers ?: emptyMap()
        
        boardConfig.powerplants.forEach { (powerplantName, quantity) ->
            if (quantity > 0) {
                val coefficient = productionCoefficients[powerplantName] ?: 0.0
                if (coefficient > 0) {
                    val range = powerplantRanges[powerplantName] ?: powerplantRanges[powerplantName.uppercase()]
                    val maxOutput = range?.max ?: 1.0
                    val minOutput = range?.min ?: 0.0
                    
                    val savedProductionLevel = boardConfigRepository.getProductionLevel(boardId, powerplantName)
                    val maxProductionForQuantity = (maxOutput * coefficient) * quantity
                    val currentProductionLevel = savedProductionLevel ?: maxProductionForQuantity
                    
                    val minProductionForQuantity = (minOutput * coefficient) * quantity
                    val actualProduction = currentProductionLevel.coerceIn(minProductionForQuantity, maxProductionForQuantity)
                    
                    totalProduction += actualProduction
                    
                    val powerplantKey = powerplantName.uppercase() // API expects uppercase keys
                    powerGenerationByType[powerplantKey] = (powerGenerationByType[powerplantKey] ?: 0.0) + actualProduction
                }
            }
        }
        
        boardConfig.buildings.forEach { (buildingName, quantity) ->
            if (quantity > 0) {
                // Prioritize exact match, then API standard format for consumption calculation
                val consumption = consumptionModifiers[buildingName] 
                    ?: consumptionModifiers[buildingName.uppercase().replace(" ", "_")] 
                    ?: 0.0 
                totalConsumption += consumption * quantity
            }
        }
        
        val productionInt = totalProduction.toInt()
        val consumptionInt = totalConsumption.toInt()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Submit Board Data")
            .setMessage("""
                Submit the following data to server?
                
                Board ID: $boardId
                Production: $productionInt MW
                Consumption: $consumptionInt MW
                Power Generation:
                ${powerGenerationByType.entries.joinToString("\n") { "  ${it.key}: ${String.format("%.1f",it.value)} MW" }}
            """.trimIndent())
            .setPositiveButton("Submit") { _, _ ->
                settingsViewModel.submitBoardData(
                    boardId = boardId,
                    production = productionInt,
                    consumption = consumptionInt,
                    powerGenerationByType = powerGenerationByType.mapValues { it.value } // Ensure Double for GSON
                )
                Toast.makeText(this, "Submitting board data...", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
