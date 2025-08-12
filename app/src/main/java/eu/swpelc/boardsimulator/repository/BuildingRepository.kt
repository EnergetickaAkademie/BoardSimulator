package eu.swpelc.boardsimulator.repository

import eu.swpelc.boardsimulator.data.BuildingDao
import eu.swpelc.boardsimulator.model.BuildingItem
import kotlinx.coroutines.flow.Flow

class BuildingRepository(private val buildingDao: BuildingDao) {
    
    fun getAllBuildings(): Flow<List<BuildingItem>> = buildingDao.getAllBuildings()
    
    fun getActiveBuildings(): Flow<List<BuildingItem>> = buildingDao.getActiveBuildings()
    
    suspend fun insertBuilding(building: BuildingItem) = buildingDao.insertBuilding(building)
    
    suspend fun updateBuilding(building: BuildingItem) = buildingDao.updateBuilding(building)
    
    suspend fun deleteBuilding(building: BuildingItem) = buildingDao.deleteBuilding(building)
    
    suspend fun deleteBuildingById(id: Long) = buildingDao.deleteBuildingById(id)
}
