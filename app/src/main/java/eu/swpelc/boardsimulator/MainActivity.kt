package eu.swpelc.boardsimulator

import android.os.Bundle
import android.view.Menu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import eu.swpelc.boardsimulator.databinding.ActivityMainBinding
import eu.swpelc.boardsimulator.service.BoardSubmissionService
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var boardSubmissionService: BoardSubmissionService
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        // Initialize global board submission service
        boardSubmissionService = BoardSubmissionService.getInstance(this)
        
        // Initialize settings view model to monitor settings changes
        val repository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        
        // Monitor settings changes and update submission service
        lifecycleScope.launch {
            settingsViewModel.serverSettings.collect { settings ->
                boardSubmissionService.updateInterval(settings.refreshInterval)
            }
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_slideshow, R.id.nav_board_config
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        boardSubmissionService.startPeriodicSubmission()
    }

    override fun onPause() {
        super.onPause()
        boardSubmissionService.stopPeriodicSubmission()
    }

    override fun onDestroy() {
        super.onDestroy()
        boardSubmissionService.cleanup()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}