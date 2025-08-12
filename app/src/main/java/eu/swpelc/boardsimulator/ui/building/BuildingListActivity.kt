package eu.swpelc.boardsimulator.ui.building

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.ActivityBuildingListBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import eu.swpelc.boardsimulator.model.BoardBuildingItem
import kotlinx.coroutines.launch

class BuildingListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityBuildingListBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var buildingListAdapter: BuildingListAdapter
    private var boardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBuildingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        boardId = intent.getStringExtra(EXTRA_BOARD_ID) ?: ""
        
        setupActionBar()
        setupRepositories()
        setupViewModel()
        setupRecyclerView()
        setupObservers()
        
        // Fetch simulation dump on startup
        settingsViewModel.fetchSimulationDump()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Buildings - Board $boardId"
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
        buildingListAdapter = BuildingListAdapter()
        
        binding.recyclerViewBuildings.apply {
            layoutManager = LinearLayoutManager(this@BuildingListActivity)
            adapter = buildingListAdapter
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
                    // Update the list when quantities change
                    val dump = settingsViewModel.simulationDump.value
                    val consumptionModifiers = dump?.groups?.values?.firstOrNull()?.consumption_modifiers
                    if (consumptionModifiers != null) {
                        updateBuildingsList(consumptionModifiers)
                    }
                }
            }
        }
    }

    private fun updateBuildingsList(consumptionModifiers: Map<String, Double>) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val buildingItems = boardConfig.buildings.map { (name, quantity) ->
            val consumption = consumptionModifiers[name] ?: 0.0
            BoardBuildingItem(name, consumption, quantity)
        }.sortedBy { it.name }
        
        buildingListAdapter.updateBuildings(buildingItems)
        
        // Update empty state
        if (buildingItems.isEmpty()) {
            binding.textEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewBuildings.visibility = android.view.View.GONE
        } else {
            binding.textEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewBuildings.visibility = android.view.View.VISIBLE
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
