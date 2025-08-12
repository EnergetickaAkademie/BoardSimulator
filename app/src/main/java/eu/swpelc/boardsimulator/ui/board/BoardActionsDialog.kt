package eu.swpelc.boardsimulator.ui.board

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BoardActionsDialog : DialogFragment() {

    companion object {
        private const val ARG_BOARD_ID = "board_id"
        
        fun newInstance(boardId: String): BoardActionsDialog {
            val fragment = BoardActionsDialog()
            val args = Bundle()
            args.putString(ARG_BOARD_ID, boardId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val boardId = arguments?.getString(ARG_BOARD_ID) ?: "Unknown"
        
        val actions = arrayOf(
            "Adjust Production",
            "Add Buildings", 
            "Add Powerplant"
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Board $boardId Actions")
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showAdjustProductionDialog(boardId)
                    1 -> showAddBuildingsDialog(boardId)
                    2 -> showAddPowerplantDialog(boardId)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }

    private fun showAdjustProductionDialog(boardId: String) {
        val dialog = AdjustProductionDialog.newInstance(boardId)
        dialog.show(parentFragmentManager, "AdjustProductionDialog")
    }

    private fun showAddBuildingsDialog(boardId: String) {
        val dialog = AddBuildingsDialog.newInstance(boardId)
        dialog.show(parentFragmentManager, "AddBuildingsDialog")
    }

    private fun showAddPowerplantDialog(boardId: String) {
        val dialog = AddPowerplantDialog.newInstance(boardId)
        dialog.show(parentFragmentManager, "AddPowerplantDialog")
    }
}
