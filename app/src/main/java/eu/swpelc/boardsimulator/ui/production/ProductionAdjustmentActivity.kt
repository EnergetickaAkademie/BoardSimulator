package eu.swpelc.boardsimulator.ui.production

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.ActivityProductionAdjustmentBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class ProductionAdjustmentActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityProductionAdjustmentBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var productionAdapter: ProductionSliderAdapter
    private var boardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityProductionAdjustmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        boardId = intent.getStringExtra(EXTRA_BOARD_ID) ?: ""
        
        setupActionBar()
        setupRepositories()
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        
        // Fetch simulation dump on startup
        refreshData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        refreshData()
    }

    private fun refreshData() {
        settingsViewModel.fetchSimulationDump()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Adjust Production - Board $boardId"
        }
    }

    private fun setupRepositories() {
        boardConfigRepository = BoardConfigRepository.getInstance(this)
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        productionAdapter = ProductionSliderAdapter { powerplantName, newProduction ->
            // Save production adjustment to local storage
            boardConfigRepository.setProductionLevel(boardId, powerplantName, newProduction)
        }
        
        binding.recyclerViewProduction.apply {
            layoutManager = LinearLayoutManager(this@ProductionAdjustmentActivity)
            adapter = productionAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.simulationDump.collect { dump ->
                dump?.let {
                    val productionCoefficients = dump.groups.values.firstOrNull()?.production_coefficients
                    if (productionCoefficients != null) {
                        updateProductionList(productionCoefficients)
                    }
                }
            }
        }

        lifecycleScope.launch {
            boardConfigRepository.boardConfigurations.collect { configurations ->
                val boardConfig = configurations[boardId]
                boardConfig?.let {
                    // Update the list when quantities change
                    val dump = settingsViewModel.simulationDump.value
                    val productionCoefficients = dump?.groups?.values?.firstOrNull()?.production_coefficients
                    if (productionCoefficients != null) {
                        updateProductionList(productionCoefficients)
                    }
                }
            }
        }
    }

    private fun updateProductionList(productionCoefficients: Map<String, Double>) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val dump = settingsViewModel.simulationDump.value
        val powerplantRanges = dump?.groups?.values?.firstOrNull()?.powerplant_ranges
        
        val productionData = boardConfig.powerplants.mapNotNull { (name, quantity) ->
            if (quantity > 0) {
                val coefficient = productionCoefficients[name] ?: 0.0
                val isRenewable = name.lowercase().contains("wind") || name.lowercase().contains("photovoltaic")
                
                // Find the correct range - try both the exact name and uppercase version
                val range = powerplantRanges?.get(name) ?: powerplantRanges?.get(name.uppercase())
                
                // Calculate min/max production based on powerplant ranges and coefficients
                val minProduction = if (isRenewable) {
                    // For renewable sources, production is fixed at coefficient * quantity
                    coefficient * quantity
                } else {
                    // For controllable sources, use minimum from range
                    val rangeMin = range?.min ?: 0.0
                    rangeMin * coefficient * quantity
                }
                
                val maxProduction = if (range != null) {
                    range.max * coefficient * quantity
                } else {
                    // If no range found, default based on coefficient
                    if (coefficient > 0) coefficient * quantity else 0.0
                }
                
                // Only include items with non-zero max production or renewable sources
                if (maxProduction > 0 || isRenewable) {
                    // Load saved production level or default to max
                    val savedProductionLevel = boardConfigRepository.getProductionLevel(boardId, name)
                    val currentProduction = savedProductionLevel ?: maxProduction
                    
                    ProductionSliderItem(
                        powerplantName = name,
                        quantity = quantity,
                        coefficient = coefficient,
                        minProduction = minProduction,
                        maxProduction = maxProduction,
                        currentProduction = currentProduction,
                        isRenewable = isRenewable
                    )
                } else {
                    null // Don't show powerplants with 0 max production (like Gas with coefficient 0)
                }
            } else null
        }
        
        productionAdapter.updateProduction(productionData)
        
        // Update empty state
        if (productionData.isEmpty()) {
            binding.textEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewProduction.visibility = android.view.View.GONE
        } else {
            binding.textEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewProduction.visibility = android.view.View.VISIBLE
        }
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

data class ProductionSliderItem(
    val powerplantName: String,
    val quantity: Int,
    val coefficient: Double,
    val minProduction: Double,
    val maxProduction: Double,
    val currentProduction: Double,
    val isRenewable: Boolean
)
