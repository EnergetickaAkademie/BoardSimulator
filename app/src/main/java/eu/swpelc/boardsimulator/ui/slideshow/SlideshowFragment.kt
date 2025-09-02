package eu.swpelc.boardsimulator.ui.slideshow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import eu.swpelc.boardsimulator.R
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
                updateBoardLoginButtonState()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.loginToken.collect { token ->
                updateLoginButtonState()
                updateSimulationButtonState()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.boardTokens.collect { tokens ->
                updateBoardLoginButtonState()
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                binding.buttonSave.isEnabled = !isLoading
                binding.buttonLogin.isEnabled = !isLoading
                binding.buttonLoginBoards.isEnabled = !isLoading && currentSettings?.serverIp?.isNotEmpty() == true
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

        lifecycleScope.launch {
            settingsViewModel.boardLoginResult.collect { result ->
                result?.let {
                    if (it.isSuccess) {
                        val successfulLogins = it.getOrNull() ?: emptyMap()
                        val totalBoards = currentSettings?.boardUsernames?.split(",")?.size ?: 0
                        Toast.makeText(
                            context, 
                            "Board login & registration completed! ${successfulLogins.size}/$totalBoards boards active", 
                            Toast.LENGTH_LONG
                        ).show()
                        updateBoardLoginButtonState()
                    } else {
                        Toast.makeText(
                            context, 
                            "Board login failed: ${it.exceptionOrNull()?.message}", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    settingsViewModel.clearBoardLoginResult()
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

        binding.buttonLoginBoards.setOnClickListener {
            loginBoards()
        }

        binding.buttonConfigureBoardCredentials.setOnClickListener {
            showBoardCredentialsDialog()
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
            boardCredentials = currentSettings?.boardCredentials ?: emptyMap(),
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

    private fun loginBoards() {
        currentSettings?.let { settings ->
            if (settings.serverIp.isEmpty()) {
                Toast.makeText(context, "Please configure server IP first", Toast.LENGTH_SHORT).show()
                return
            }
            if (settings.boardUsernames.isEmpty()) {
                Toast.makeText(context, "Please configure board usernames first", Toast.LENGTH_SHORT).show()
                return
            }
            settingsViewModel.loginBoards()
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

    private fun updateBoardLoginButtonState() {
        val boardCount = settingsViewModel.getLoggedInBoardCount()
        val totalBoards = currentSettings?.boardUsernames?.split(",")?.filter { it.trim().isNotEmpty() }?.size ?: 0
        
        if (boardCount > 0) {
            binding.buttonLoginBoards.text = "Boards Active ($boardCount/$totalBoards)"
        } else {
            binding.buttonLoginBoards.text = "Login & Register Boards"
        }
        
        binding.buttonLoginBoards.isEnabled = currentSettings?.serverIp?.isNotEmpty() == true && 
                                              currentSettings?.boardUsernames?.isNotEmpty() == true
    }

    private fun showBoardCredentialsDialog() {
        val settings = currentSettings ?: return
        val boardUsernames = settings.boardUsernames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        
        if (boardUsernames.isEmpty()) {
            Toast.makeText(context, "Please configure board usernames first", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_board_credentials, null)
        val layoutCredentials = dialogView.findViewById<LinearLayout>(R.id.layout_board_credentials)
        
        val editTexts = mutableMapOf<String, EditText>()
        
        // Create input fields for each board
        for (username in boardUsernames) {
            val textInputLayout = TextInputLayout(requireContext()).apply {
                hint = "$username Password"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 32
                }
            }
            
            val editText = TextInputEditText(requireContext()).apply {
                setText(settings.boardCredentials[username] ?: "board123")
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            
            textInputLayout.addView(editText)
            layoutCredentials.addView(textInputLayout)
            editTexts[username] = editText
        }
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<View>(R.id.button_cancel).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.button_save_credentials).setOnClickListener {
            val newCredentials = mutableMapOf<String, String>()
            
            for ((username, editText) in editTexts) {
                val password = editText.text.toString().trim()
                if (password.isNotEmpty()) {
                    newCredentials[username] = password
                }
            }
            
            // Update settings with new board credentials
            val updatedSettings = settings.copy(boardCredentials = newCredentials)
            settingsViewModel.saveSettings(updatedSettings)
            
            Toast.makeText(context, "Board credentials saved successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}