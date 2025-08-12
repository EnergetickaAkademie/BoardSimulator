package eu.swpelc.boardsimulator.ui.production

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemProductionSliderBinding
import kotlin.math.roundToInt

class ProductionSliderAdapter(
    private val onProductionChanged: (String, Double) -> Unit
) : ListAdapter<ProductionSliderItem, ProductionSliderAdapter.ProductionViewHolder>(ProductionDiffCallback()) {

    fun updateProduction(newList: List<ProductionSliderItem>) {
        submitList(newList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductionViewHolder {
        val binding = ItemProductionSliderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductionViewHolder(
        private val binding: ItemProductionSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProductionSliderItem) {
            binding.apply {
                textPowerplantName.text = item.powerplantName
                textQuantity.text = "Quantity: ${item.quantity}"
                
                if (item.isRenewable) {
                    // For renewable sources, show only the fixed value
                    seekBarProduction.visibility = android.view.View.GONE
                    textProductionValue.text = String.format("%.2f MW (Fixed)", item.maxProduction)
                    textProductionRange.text = "Renewable energy - production varies with conditions"
                } else {
                    // For controllable sources, show slider
                    seekBarProduction.visibility = android.view.View.VISIBLE
                    
                    // Set up the slider
                    val range = (item.maxProduction - item.minProduction).toInt()
                    seekBarProduction.max = if (range > 0) range else 1
                    seekBarProduction.progress = ((item.currentProduction - item.minProduction).roundToInt())
                    
                    // Update text displays
                    updateProductionDisplay(item, item.currentProduction)
                    
                    // Set up slider listener
                    seekBarProduction.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val newProduction = item.minProduction + progress
                                updateProductionDisplay(item, newProduction)
                                onProductionChanged(item.powerplantName, newProduction)
                            }
                        }
                        
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }
            }
        }
        
        private fun updateProductionDisplay(item: ProductionSliderItem, currentProduction: Double) {
            binding.textProductionValue.text = String.format("%.2f MW", currentProduction)
            binding.textProductionRange.text = String.format(
                "Range: %.2f - %.2f MW",
                item.minProduction,
                item.maxProduction
            )
        }
    }

    private class ProductionDiffCallback : DiffUtil.ItemCallback<ProductionSliderItem>() {
        override fun areItemsTheSame(oldItem: ProductionSliderItem, newItem: ProductionSliderItem): Boolean {
            return oldItem.powerplantName == newItem.powerplantName
        }

        override fun areContentsTheSame(oldItem: ProductionSliderItem, newItem: ProductionSliderItem): Boolean {
            return oldItem == newItem
        }
    }
}
