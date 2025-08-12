package eu.swpelc.boardsimulator.ui.building

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemBuildingSelectionBinding

class BuildingAdapter(
    private val onBuildingClick: (String, Double) -> Unit
) : RecyclerView.Adapter<BuildingAdapter.BuildingViewHolder>() {

    private var buildings: Map<String, Double> = emptyMap()

    fun updateBuildings(newBuildings: Map<String, Double>) {
        buildings = newBuildings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val binding = ItemBuildingSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BuildingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        val buildingEntry = buildings.entries.elementAt(position)
        holder.bind(buildingEntry.key, buildingEntry.value)
    }

    override fun getItemCount(): Int = buildings.size

    inner class BuildingViewHolder(private val binding: ItemBuildingSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(buildingName: String, consumption: Double) {
            binding.textBuildingName.text = buildingName.replace("_", " ")
            binding.textConsumption.text = "Consumption: ${consumption.toInt()}"
            
            binding.root.setOnClickListener {
                onBuildingClick(buildingName, consumption)
            }
        }
    }
}
