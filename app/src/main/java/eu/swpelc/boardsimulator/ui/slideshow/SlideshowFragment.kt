package eu.swpelc.boardsimulator.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import eu.swpelc.boardsimulator.databinding.FragmentSlideshowBinding
import eu.swpelc.boardsimulator.model.ServerSettings
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import eu.swpelc.boardsimulator.ui.simulation.SimulationActivity
import kotlinx.coroutines.launch

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsViewModel: SettingsViewModel
    private var currentSettings: ServerSettings? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModel()
        setupObservers()
        setupClickListeners()

        return root
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(requireContext())
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.serverSettings.collect { settings ->
                currentSettings = settings
                binding.editServerIp.setText(settings.serverIp)
                binding.editBoardUsernames.setText(settings.boardUsernames)
                binding.editRefreshInterval.setText(settings.refreshInterval.toString())
                binding.editLecturerUsername.setText(settings.lecturerUsername)
                binding.editLecturerPassword.setText(settings.lecturerPassword)
                updateLoginButtonState()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.loginToken.collect { token ->
                updateLoginButtonState()
                updateSimulationButtonState()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                binding.buttonSave.isEnabled = !isLoading
                binding.buttonLogin.isEnabled = !isLoading
                binding.buttonViewSimulation.isEnabled = !isLoading && settingsViewModel.isLoggedIn()
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            settingsViewModel.loginResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        val loginResponse = it.getOrNull()
                        Toast.makeText(
                            context, 
                            "Login successful! Welcome ${loginResponse?.username}", 
                            Toast.LENGTH_SHORT
                        ).show()
                        updateLoginButtonState()
                        updateSimulationButtonState()
                    } else {
                        Toast.makeText(
                            context, 
                            "Login failed: ${it.exceptionOrNull()?.message}", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    settingsViewModel.clearLoginResult()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonSave.setOnClickListener {
            saveServerSettings()
        }
        
        binding.buttonLogin.setOnClickListener {
            if (settingsViewModel.isLoggedIn()) {
                logout()
            } else {
                login()
            }
        }

        binding.buttonViewSimulation.setOnClickListener {
            openSimulationViewer()
        }
    }

    private fun saveServerSettings() {
        val serverIp = binding.editServerIp.text.toString().trim()
        val boardUsernames = binding.editBoardUsernames.text.toString().trim()
        val refreshIntervalText = binding.editRefreshInterval.text.toString().trim()
        val lecturerUsername = binding.editLecturerUsername.text.toString().trim()
        val lecturerPassword = binding.editLecturerPassword.text.toString().trim()

        if (serverIp.isEmpty()) {
            Toast.makeText(context, "Please enter server IP address", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse refresh interval with validation
        val refreshInterval = try {
            if (refreshIntervalText.isEmpty()) {
                10000L // Default value
            } else {
                val value = refreshIntervalText.toLong()
                if (value < 1000L) {
                    Toast.makeText(context, "Refresh interval must be at least 1000ms", Toast.LENGTH_SHORT).show()
                    return
                }
                value
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Please enter a valid number for refresh interval", Toast.LENGTH_SHORT).show()
            return
        }

        val settings = ServerSettings(
            serverIp = serverIp,
            lecturerUsername = lecturerUsername.ifEmpty { "lecturer1" },
            lecturerPassword = lecturerPassword.ifEmpty { "lecturer123" },
            boardUsernames = boardUsernames.ifEmpty { "board1, board2, board3" },
            refreshInterval = refreshInterval
        )

        settingsViewModel.saveSettings(settings)
        Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }

    private fun login() {
        currentSettings?.let { settings ->
            if (settings.serverIp.isEmpty()) {
                Toast.makeText(context, "Please configure server IP first", Toast.LENGTH_SHORT).show()
                return
            }
            settingsViewModel.login()
        }
    }

    private fun logout() {
        settingsViewModel.logout()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun openSimulationViewer() {
        val intent = Intent(context, SimulationActivity::class.java)
        startActivity(intent)
    }

    private fun updateLoginButtonState() {
        val isLoggedIn = settingsViewModel.isLoggedIn()
        binding.buttonLogin.text = if (isLoggedIn) "Logout" else "Login"
        binding.buttonLogin.isEnabled = currentSettings?.serverIp?.isNotEmpty() == true
    }

    private fun updateSimulationButtonState() {
        binding.buttonViewSimulation.isEnabled = settingsViewModel.isLoggedIn()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}