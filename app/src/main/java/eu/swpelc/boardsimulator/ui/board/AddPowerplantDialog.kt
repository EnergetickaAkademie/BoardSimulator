package eu.swpelc.boardsimulator.ui.board

import android.content.Intent
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import eu.swpelc.boardsimulator.ui.powerplant.PowerplantSelectionActivity

class AddPowerplantDialog : DialogFragment() {

    companion object {
        private const val ARG_BOARD_ID = "board_id"
        
        fun newInstance(boardId: String): AddPowerplantDialog {
            val fragment = AddPowerplantDialog()
            val args = Bundle()
            args.putString(ARG_BOARD_ID, boardId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val boardId = arguments?.getString(ARG_BOARD_ID) ?: "Unknown"
        
        // Instead of showing a dialog, navigate to the PowerplantSelectionActivity
        val intent = Intent(requireContext(), PowerplantSelectionActivity::class.java)
        intent.putExtra(PowerplantSelectionActivity.EXTRA_BOARD_ID, boardId)
        startActivity(intent)
        
        // Dismiss this dialog immediately
        dismiss()
        
        // Return a dummy dialog (won't be shown)
        return MaterialAlertDialogBuilder(requireContext())
            .create()
    }
}
