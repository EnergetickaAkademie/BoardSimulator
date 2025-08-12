package eu.swpelc.boardsimulator.data

import androidx.room.*
import eu.swpelc.boardsimulator.model.BuildingItem
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildingDao {
    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun getAllBuildings(): Flow<List<BuildingItem>>

    @Query("SELECT * FROM buildings WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveBuildings(): Flow<List<BuildingItem>>

    @Insert
    suspend fun insertBuilding(building: BuildingItem)

    @Update
    suspend fun updateBuilding(building: BuildingItem)

    @Delete
    suspend fun deleteBuilding(building: BuildingItem)

    @Query("DELETE FROM buildings WHERE id = :id")
    suspend fun deleteBuildingById(id: Long)
}
