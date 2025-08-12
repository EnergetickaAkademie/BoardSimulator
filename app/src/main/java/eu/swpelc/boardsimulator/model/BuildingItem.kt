package eu.swpelc.boardsimulator.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings")
data class BuildingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val building: Building,
    val name: String,
    val description: String = "",
    val isActive: Boolean = true
)
