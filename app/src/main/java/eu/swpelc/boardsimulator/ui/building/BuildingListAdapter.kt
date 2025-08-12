package eu.swpelc.boardsimulator.ui.building

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemBuildingListBinding
import eu.swpelc.boardsimulator.model.BoardBuildingItem

class BuildingListAdapter : RecyclerView.Adapter<BuildingListAdapter.BuildingViewHolder>() {

    private var buildings = listOf<BoardBuildingItem>()

    fun updateBuildings(newBuildings: List<BoardBuildingItem>) {
        buildings = newBuildings
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val binding = ItemBuildingListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BuildingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        holder.bind(buildings[position])
    }

    override fun getItemCount() = buildings.size

    class BuildingViewHolder(private val binding: ItemBuildingListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(buildingItem: BoardBuildingItem) {
            binding.apply {
                textBuildingName.text = buildingItem.name
                textBuildingConsumption.text = "Consumption: ${buildingItem.consumption}"
                textBuildingQuantity.text = "Quantity: ${buildingItem.quantity}"
            }
        }
    }
}
