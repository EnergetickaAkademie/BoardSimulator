package eu.swpelc.boardsimulator.ui.boarddetail

import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

class BoardDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityBoardDetailBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private var boardId: String = ""
    private var periodicRefreshJob: Job? = null
    private var lastBoardData: eu.swpelc.boardsimulator.model.BoardData? = null
    private var isPeriodicRefresh = false
    private var currentRefreshInterval = 10000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBoardDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        boardId = intent.getStringExtra(EXTRA_BOARD_ID) ?: ""
        boardConfigRepository = BoardConfigRepository(this)
        
        setupActionBar()
        setupViewModel()
        setupObservers()
        setupClickListeners()
        
        // Fetch simulation dump on startup
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        refreshData()
        startPeriodicRefresh()
    }

    override fun onPause() {
        super.onPause()
        stopPeriodicRefresh()
    }

    private fun startPeriodicRefresh() {
        stopPeriodicRefresh() // Stop any existing refresh job
        periodicRefreshJob = lifecycleScope.launch {
            while (true) {
                delay(currentRefreshInterval)
                isPeriodicRefresh = true
                refreshData()
                isPeriodicRefresh = false
            }
        }
    }

    private fun stopPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
    }

    private fun refreshData() {
        settingsViewModel.fetchSimulationDump()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Board $boardId Configuration"
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
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                // Hide loading indicator during periodic refresh to prevent blinking
                val shouldShowLoading = isLoading && !isPeriodicRefresh
                binding.progressBar.visibility = if (shouldShowLoading) android.view.View.VISIBLE else android.view.View.INVISIBLE
            }
        }

        lifecycleScope.launch {
            settingsViewModel.serverSettings.collect { settings ->
                if (currentRefreshInterval != settings.refreshInterval) {
                    currentRefreshInterval = settings.refreshInterval
                    // Restart periodic refresh with new interval
                    if (periodicRefreshJob?.isActive == true) {
                        startPeriodicRefresh()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonAdjustProduction.setOnClickListener {
            isPeriodicRefresh = false
            refreshData() // Refresh before opening production adjustment
            val intent = Intent(this, eu.swpelc.boardsimulator.ui.production.ProductionAdjustmentActivity::class.java)
            intent.putExtra(eu.swpelc.boardsimulator.ui.production.ProductionAdjustmentActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonAddBuildings.setOnClickListener {
            isPeriodicRefresh = false
            refreshData() // Refresh before opening building selection
            val intent = Intent(this, BuildingSelectionActivity::class.java)
            intent.putExtra(BuildingSelectionActivity.EXTRA_BOARD_ID, boardId)
            startActivity(intent)
        }

        binding.buttonAddPowerplants.setOnClickListener {
            isPeriodicRefresh = false
            refreshData() // Refresh before opening powerplant selection
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
        
        // Add refresh button functionality if it exists
        binding.buttonRefresh?.setOnClickListener {
            isPeriodicRefresh = false
            refreshData()
            Toast.makeText(this, "Refreshing board data...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Board")
            .setMessage("Are you sure you want to reset all buildings and powerplants for Board $boardId? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                resetBoard()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetBoard() {
        boardConfigRepository.resetBoard(boardId)
        Toast.makeText(this, "Board $boardId has been reset", Toast.LENGTH_SHORT).show()
        
        // Refresh the simulation dump to update the display
        settingsViewModel.fetchSimulationDump()
    }

    private fun updateBoardInfo(boardData: eu.swpelc.boardsimulator.model.BoardData) {
        // Only update UI if data has actually changed to prevent unnecessary layout shifts
        if (lastBoardData?.let { 
            it.current_production == boardData.current_production &&
            it.current_consumption == boardData.current_consumption
        } == true) {
            return
        }
        
        lastBoardData = boardData
        
        // Use fixed-width formatting to prevent layout shifts
        val production = boardData.current_production.toInt()
        val consumption = boardData.current_consumption.toInt()
        val balance = production - consumption
        
        // Format with consistent width to prevent text jumping
        binding.textCurrentProduction.text = String.format("Current Production: %4d MW", production)
        binding.textCurrentConsumption.text = String.format("Current Consumption: %4d MW", consumption)
        binding.textBalance.text = String.format("Balance: %+5d MW", balance)
        
        val isActive = boardData.current_production > 0 || boardData.current_consumption > 0
        binding.textStatus.text = "Status: ${if (isActive) "Active  " else "Inactive"}"
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
