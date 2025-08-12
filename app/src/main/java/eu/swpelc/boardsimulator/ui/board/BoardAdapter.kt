package eu.swpelc.boardsimulator.ui.board

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import eu.swpelc.boardsimulator.databinding.ItemBoardBinding
import eu.swpelc.boardsimulator.model.BoardData

class BoardAdapter(
    private val onBoardClick: (String) -> Unit
) : RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {

    private var boards: Map<String, BoardData> = emptyMap()

    fun updateBoards(newBoards: Map<String, BoardData>) {
        boards = newBoards
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val binding = ItemBoardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val boardEntry = boards.entries.elementAt(position)
        holder.bind(boardEntry.key, boardEntry.value)
    }

    override fun getItemCount(): Int = boards.size

    inner class BoardViewHolder(private val binding: ItemBoardBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(boardId: String, boardData: BoardData) {
            binding.textBoardId.text = "Board $boardId"
            binding.textProduction.text = "Production: ${boardData.production.toInt()}"
            binding.textConsumption.text = "Consumption: ${boardData.consumption.toInt()}"
            
            val isActive = boardData.production > 0 || boardData.consumption > 0
            binding.textStatus.text = if (isActive) "Active" else "Inactive"
            
            binding.root.setOnClickListener {
                onBoardClick(boardId)
            }
        }
    }
}
