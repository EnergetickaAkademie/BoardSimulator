package eu.swpelc.boardsimulator.ui.powerplant

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.ActivityPowerplantListBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import eu.swpelc.boardsimulator.model.PowerplantItem
import kotlinx.coroutines.launch

class PowerplantListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityPowerplantListBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var powerplantListAdapter: PowerplantListAdapter
    private var boardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPowerplantListBinding.inflate(layoutInflater)
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
            title = "Powerplants - Board $boardId"
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
        powerplantListAdapter = PowerplantListAdapter()
        
        binding.recyclerViewPowerplants.apply {
            layoutManager = LinearLayoutManager(this@PowerplantListActivity)
            adapter = powerplantListAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.simulationDump.collect { dump ->
                dump?.let {
                    val productionCoefficients = dump.groups.values.firstOrNull()?.production_coefficients
                    if (productionCoefficients != null) {
                        updatePowerplantsList(productionCoefficients)
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
                        updatePowerplantsList(productionCoefficients)
                    }
                }
            }
        }
    }

    private fun updatePowerplantsList(productionCoefficients: Map<String, Double>) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val powerplantItems = boardConfig.powerplants.map { (name, quantity) ->
            val coefficient = productionCoefficients[name] ?: 0.0
            PowerplantItem(name, coefficient, quantity)
        }.sortedBy { it.name }
        
        powerplantListAdapter.updatePowerplants(powerplantItems)
        
        // Update empty state
        if (powerplantItems.isEmpty()) {
            binding.textEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewPowerplants.visibility = android.view.View.GONE
        } else {
            binding.textEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewPowerplants.visibility = android.view.View.VISIBLE
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
