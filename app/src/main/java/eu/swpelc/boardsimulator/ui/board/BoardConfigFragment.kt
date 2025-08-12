package eu.swpelc.boardsimulator.ui.board

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import eu.swpelc.boardsimulator.databinding.FragmentBoardConfigBinding
import eu.swpelc.boardsimulator.repository.SettingsRepository
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModel
import eu.swpelc.boardsimulator.ui.settings.SettingsViewModelFactory
import kotlinx.coroutines.launch

class BoardConfigFragment : Fragment() {

    private var _binding: FragmentBoardConfigBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var boardAdapter: BoardAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardConfigBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        return root
    }

    private fun setupViewModel() {
        val repository = SettingsRepository(requireContext())
        val factory = SettingsViewModelFactory(repository)
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        boardAdapter = BoardAdapter { boardId ->
            // Handle board click - show board actions
            showBoardActions(boardId)
        }
        
        binding.recyclerViewBoards.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = boardAdapter
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            settingsViewModel.simulationDump.collect { dump ->
                dump?.let {
                    val boards = dump.groups.values.firstOrNull()?.boards ?: emptyMap()
                    boardAdapter.updateBoards(boards)
                }
            }
        }

        lifecycleScope.launch {
            settingsViewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.buttonRefresh.isEnabled = !isLoading
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonRefresh.setOnClickListener {
            settingsViewModel.fetchSimulationDump()
        }
    }

    private fun showBoardActions(boardId: String) {
        val intent = android.content.Intent(requireContext(), eu.swpelc.boardsimulator.ui.boarddetail.BoardDetailActivity::class.java)
        intent.putExtra(eu.swpelc.boardsimulator.ui.boarddetail.BoardDetailActivity.EXTRA_BOARD_ID, boardId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Fetch fresh data when fragment becomes visible
        settingsViewModel.fetchSimulationDump()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
