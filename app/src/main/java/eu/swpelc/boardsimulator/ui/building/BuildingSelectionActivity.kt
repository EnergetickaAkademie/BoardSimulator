package eu.swpelc.boardsimulator.ui.building

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.ActivityBuildingSelectionBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class BuildingSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityBuildingSelectionBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var buildingAdapter: BuildingQuantityAdapter
    private var boardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBuildingSelectionBinding.inflate(layoutInflater)
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
            title = "Add Building - Board $boardId"
        }
    }

    private fun setupRepositories() {
        boardConfigRepository = BoardConfigRepository(this)
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        buildingAdapter = BuildingQuantityAdapter(
            onQuantityChanged = { buildingName, quantity ->
                boardConfigRepository.setBuildingQuantity(boardId, buildingName, quantity)
            }
        )
        
        binding.recyclerViewBuildings.apply {
            layoutManager = LinearLayoutManager(this@BuildingSelectionActivity)
            adapter = buildingAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.simulationDump.collect { dump ->
                dump?.let {
                    val consumptionModifiers = dump.groups.values.firstOrNull()?.consumption_modifiers
                    if (consumptionModifiers != null) {
                        updateBuildingsList(consumptionModifiers)
                    }
                }
            }
        }

        lifecycleScope.launch {
            boardConfigRepository.boardConfigurations.collect { configurations ->
                val boardConfig = configurations[boardId]
                boardConfig?.let {
                    // Update adapter with current quantities
                    buildingAdapter.updateQuantities(it.buildings)
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        lifecycleScope.launch {
            settingsViewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(this@BuildingSelectionActivity, "Error: $it", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateBuildingsList(consumptionModifiers: Map<String, Double>) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        buildingAdapter.updateBuildings(consumptionModifiers, boardConfig.buildings)
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
