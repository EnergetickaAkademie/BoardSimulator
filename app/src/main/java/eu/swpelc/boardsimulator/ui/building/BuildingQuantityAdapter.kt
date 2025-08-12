package eu.swpelc.boardsimulator.ui.building

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemBuildingQuantityBinding

class BuildingQuantityAdapter(
    private val onQuantityChanged: (String, Int) -> Unit
) : RecyclerView.Adapter<BuildingQuantityAdapter.BuildingViewHolder>() {

    private var buildings: Map<String, Double> = emptyMap()
    private var quantities: Map<String, Int> = emptyMap()

    fun updateBuildings(newBuildings: Map<String, Double>, newQuantities: Map<String, Int>) {
        buildings = newBuildings
        quantities = newQuantities
        notifyDataSetChanged()
    }

    fun updateQuantities(newQuantities: Map<String, Int>) {
        quantities = newQuantities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val binding = ItemBuildingQuantityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuildingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val buildingEntry = buildings.entries.elementAt(position)
        val currentQuantity = quantities[buildingEntry.key] ?: 0
        holder.bind(buildingEntry.key, buildingEntry.value, currentQuantity)
    }

    override fun getItemCount(): Int = buildings.size

    inner class BuildingViewHolder(private val binding: ItemBuildingQuantityBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(buildingName: String, consumption: Double, quantity: Int) {
            binding.textBuildingName.text = buildingName.replace("_", " ")
            binding.textConsumption.text = "Consumption: ${consumption.toInt()}"
            binding.textQuantity.text = quantity.toString()
            
            binding.buttonDecrease.setOnClickListener {
                if (quantity > 0) {
                    onQuantityChanged(buildingName, quantity - 1)
                }
            }
            
            binding.buttonIncrease.setOnClickListener {
                onQuantityChanged(buildingName, quantity + 1)
            }
        }
    }
}
