package eu.swpelc.boardsimulator.ui.powerplant

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemPowerplantSelectionBinding

class PowerplantQuantityAdapter(
    private val onQuantityChanged: (String, Int) -> Unit
) : RecyclerView.Adapter<PowerplantQuantityAdapter.PowerplantViewHolder>() {

    private var powerplants = listOf<Triple<String, Double, Int>>() // name, coefficient, quantity

    fun updatePowerplants(newPowerplants: List<Triple<String, Double, Int>>) {
        powerplants = newPowerplants
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PowerplantViewHolder {
        val binding = ItemPowerplantSelectionBinding.inflate(
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

    inner class PowerplantViewHolder(private val binding: ItemPowerplantSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(powerplantData: Triple<String, Double, Int>) {
            val (name, coefficient, quantity) = powerplantData
            
            binding.apply {
                textPowerplantName.text = name
                textPowerplantCoefficient.text = "Production: $coefficient"
                textQuantity.text = quantity.toString()
                
                buttonIncrease.setOnClickListener {
                    val newQuantity = quantity + 1
                    onQuantityChanged(name, newQuantity)
                }
                
                buttonDecrease.setOnClickListener {
                    if (quantity > 0) {
                        val newQuantity = quantity - 1
                        onQuantityChanged(name, newQuantity)
                    }
                }
            }
        }
    }
}
