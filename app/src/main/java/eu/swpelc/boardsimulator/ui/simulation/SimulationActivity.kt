package eu.swpelc.boardsimulator.ui.simulation

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import eu.swpelc.boardsimulator.databinding.ActivitySimulationBinding
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class SimulationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySimulationBinding
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySimulationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupViewModel()
        setupObservers()
        setupClickListeners()
        
        // Fetch simulation dump on startup
        settingsViewModel.fetchSimulationDump()
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Simulation Dump"
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
                    displaySimulationDump(it)
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.errorMessage.collect { error ->
                error?.let {
                    binding.textSimulationData.text = "Error: $it"
                    Toast.makeText(this@SimulationActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
                binding.buttonRefresh.isEnabled = !isLoading
                
                if (isLoading) {
                    binding.textSimulationData.text = "Loading simulation data..."
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonRefresh.setOnClickListener {
            settingsViewModel.fetchSimulationDump()
        }
    }

    private fun displaySimulationDump(dump: eu.swpelc.boardsimulator.model.SimulationDump) {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(dump)
            binding.textSimulationData.text = jsonString
        } catch (e: Exception) {
            Toast.makeText(this, "Error formatting simulation data: ${e.message}", Toast.LENGTH_LONG).show()
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
