package eu.swpelc.boardsimulator.ui.board

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AdjustProductionDialog : DialogFragment() {

    companion object {
        private const val ARG_BOARD_ID = "board_id"
        
        fun newInstance(boardId: String): AdjustProductionDialog {
            val fragment = AdjustProductionDialog()
            val args = Bundle()
            args.putString(ARG_BOARD_ID, boardId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val boardId = arguments?.getString(ARG_BOARD_ID) ?: "Unknown"
        
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val productionInput = EditText(requireContext()).apply {
            hint = "Production value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        
        val consumptionInput = EditText(requireContext()).apply {
            hint = "Consumption value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        
        layout.addView(productionInput)
        layout.addView(consumptionInput)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Adjust Production - Board $boardId")
            .setView(layout)
            .setPositiveButton("Submit") { _, _ ->
                val production = productionInput.text.toString().toIntOrNull()
                val consumption = consumptionInput.text.toString().toIntOrNull()
                
                if (production != null && consumption != null) {
                    // TODO: Submit to API
                    Toast.makeText(context, "Production: $production, Consumption: $consumption", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}
