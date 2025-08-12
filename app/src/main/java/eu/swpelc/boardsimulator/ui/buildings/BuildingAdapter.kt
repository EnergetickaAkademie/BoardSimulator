package eu.swpelc.boardsimulator.ui.buildings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.R
import eu.swpelc.boardsimulator.model.BuildingItem

class BuildingAdapter(
    private val onEditClick: (BuildingItem) -> Unit,
    private val onDeleteClick: (BuildingItem) -> Unit
) : ListAdapter<BuildingItem, BuildingAdapter.BuildingViewHolder>(BuildingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BuildingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_building, parent, false)
        return BuildingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BuildingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BuildingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.text_building_name)
        private val typeText: TextView = itemView.findViewById(R.id.text_building_type)
        private val descriptionText: TextView = itemView.findViewById(R.id.text_building_description)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(building: BuildingItem) {
            nameText.text = building.name
            typeText.text = building.building.toString()
            descriptionText.text = building.description
            
            if (building.description.isEmpty()) {
                descriptionText.visibility = View.GONE
            } else {
                descriptionText.visibility = View.VISIBLE
            }

            editButton.setOnClickListener { onEditClick(building) }
            deleteButton.setOnClickListener { onDeleteClick(building) }
        }
    }
}

class BuildingDiffCallback : DiffUtil.ItemCallback<BuildingItem>() {
    override fun areItemsTheSame(oldItem: BuildingItem, newItem: BuildingItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BuildingItem, newItem: BuildingItem): Boolean {
        return oldItem == newItem
    }
}
