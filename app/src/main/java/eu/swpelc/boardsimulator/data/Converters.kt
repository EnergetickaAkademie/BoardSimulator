package eu.swpelc.boardsimulator.data

import androidx.room.TypeConverter
import eu.swpelc.boardsimulator.model.Building

class Converters {
    @TypeConverter
    fun fromBuilding(building: Building): String {
        return building.name
    }

    @TypeConverter
    fun toBuilding(value: String): Building {
        return Building.valueOf(value)
    }
}
