package eu.swpelc.boardsimulator.ui.powerplant

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.ActivityPowerplantSelectionBinding
import eu.swpelc.boardsimulator.repository.BoardConfigRepository
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class PowerplantSelectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOARD_ID = "board_id"
    }

    private lateinit var binding: ActivityPowerplantSelectionBinding
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardConfigRepository: BoardConfigRepository
    private lateinit var powerplantAdapter: PowerplantQuantityAdapter
    private var boardId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityPowerplantSelectionBinding.inflate(layoutInflater)
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

    private fun setupRepositories() {
        boardConfigRepository = BoardConfigRepository(this)
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add Powerplant - Board $boardId"
        }
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        powerplantAdapter = PowerplantQuantityAdapter(
            onQuantityChanged = { powerplantName, quantity ->
                boardConfigRepository.setPowerplantQuantity(boardId, powerplantName, quantity)
            }
        )
        
        binding.recyclerViewPowerplants.apply {
            layoutManager = LinearLayoutManager(this@PowerplantSelectionActivity)
            adapter = powerplantAdapter
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
                    // Update the adapter when quantities change
                    val dump = settingsViewModel.simulationDump.value
                    val productionCoefficients = dump?.groups?.values?.firstOrNull()?.production_coefficients
                    if (productionCoefficients != null) {
                        updatePowerplantsList(productionCoefficients)
                    }
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
                    Toast.makeText(this@PowerplantSelectionActivity, "Error: $it", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updatePowerplantsList(productionCoefficients: Map<String, Double>) {
        val boardConfig = boardConfigRepository.getBoardConfiguration(boardId)
        val powerplantData = productionCoefficients.map { (name, coefficient) ->
            val currentQuantity = boardConfig.powerplants[name] ?: 0
            Triple(name, coefficient, currentQuantity)
        }
        powerplantAdapter.updatePowerplants(powerplantData)
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
