package eu.swpelc.boardsimulator.ui.powerplant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemPowerplantListBinding
import eu.swpelc.boardsimulator.model.PowerplantItem

class PowerplantListAdapter : RecyclerView.Adapter<PowerplantListAdapter.PowerplantViewHolder>() {

    private var powerplants = listOf<PowerplantItem>()

    fun updatePowerplants(newPowerplants: List<PowerplantItem>) {
        powerplants = newPowerplants
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PowerplantViewHolder {
        val binding = ItemPowerplantListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PowerplantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PowerplantViewHolder, position: Int) {
        holder.bind(powerplants[position])
    }

    override fun getItemCount() = powerplants.size

    class PowerplantViewHolder(private val binding: ItemPowerplantListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(powerplantItem: PowerplantItem) {
            binding.apply {
                textPowerplantName.text = powerplantItem.name
                textPowerplantCoefficient.text = "Production: ${powerplantItem.coefficient}"
                textPowerplantQuantity.text = "Quantity: ${powerplantItem.quantity}"
            }
        }
    }
}
