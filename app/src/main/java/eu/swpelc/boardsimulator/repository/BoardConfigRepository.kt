package eu.swpelc.boardsimulator.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import eu.swpelc.boardsimulator.model.BoardConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BoardConfigRepository(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("board_configs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _boardConfigurations = MutableStateFlow<Map<String, BoardConfiguration>>(loadAllConfigurations())
    val boardConfigurations: Flow<Map<String, BoardConfiguration>> = _boardConfigurations.asStateFlow()
    
    private fun loadAllConfigurations(): Map<String, BoardConfiguration> {
        val json = prefs.getString("configurations", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, BoardConfiguration>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveAllConfigurations(configurations: Map<String, BoardConfiguration>) {
        val json = gson.toJson(configurations)
        prefs.edit().putString("configurations", json).apply()
        _boardConfigurations.value = configurations
    }
    
    fun getBoardConfiguration(boardId: String): BoardConfiguration {
        return _boardConfigurations.value[boardId] ?: BoardConfiguration(boardId)
    }
    
    fun addBuilding(boardId: String, buildingName: String, quantity: Int = 1) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: BoardConfiguration(boardId)
        
        val currentBuildings = boardConfig.buildings.toMutableMap()
        currentBuildings[buildingName] = (currentBuildings[buildingName] ?: 0) + quantity
        
        val updatedConfig = boardConfig.copy(buildings = currentBuildings)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun removeBuilding(boardId: String, buildingName: String, quantity: Int = 1) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: return
        
        val currentBuildings = boardConfig.buildings.toMutableMap()
        val currentQuantity = currentBuildings[buildingName] ?: 0
        val newQuantity = (currentQuantity - quantity).coerceAtLeast(0)
        
        if (newQuantity == 0) {
            currentBuildings.remove(buildingName)
        } else {
            currentBuildings[buildingName] = newQuantity
        }
        
        val updatedConfig = boardConfig.copy(buildings = currentBuildings)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun setBuildingQuantity(boardId: String, buildingName: String, quantity: Int) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: BoardConfiguration(boardId)
        
        val currentBuildings = boardConfig.buildings.toMutableMap()
        if (quantity <= 0) {
            currentBuildings.remove(buildingName)
        } else {
            currentBuildings[buildingName] = quantity
        }
        
        val updatedConfig = boardConfig.copy(buildings = currentBuildings)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun addPowerplant(boardId: String, powerplantName: String, quantity: Int = 1) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: BoardConfiguration(boardId)
        
        val currentPowerplants = boardConfig.powerplants.toMutableMap()
        currentPowerplants[powerplantName] = (currentPowerplants[powerplantName] ?: 0) + quantity
        
        val updatedConfig = boardConfig.copy(powerplants = currentPowerplants)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun removePowerplant(boardId: String, powerplantName: String, quantity: Int = 1) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: return
        
        val currentPowerplants = boardConfig.powerplants.toMutableMap()
        val currentQuantity = currentPowerplants[powerplantName] ?: 0
        val newQuantity = (currentQuantity - quantity).coerceAtLeast(0)
        
        if (newQuantity == 0) {
            currentPowerplants.remove(powerplantName)
        } else {
            currentPowerplants[powerplantName] = newQuantity
        }
        
        val updatedConfig = boardConfig.copy(powerplants = currentPowerplants)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun setPowerplantQuantity(boardId: String, powerplantName: String, quantity: Int) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val boardConfig = configurations[boardId] ?: BoardConfiguration(boardId)
        
        val currentPowerplants = boardConfig.powerplants.toMutableMap()
        if (quantity <= 0) {
            currentPowerplants.remove(powerplantName)
        } else {
            currentPowerplants[powerplantName] = quantity
        }
        
        val updatedConfig = boardConfig.copy(powerplants = currentPowerplants)
        configurations[boardId] = updatedConfig
        saveAllConfigurations(configurations)
    }
    
    fun resetBoard(boardId: String) {
        val configurations = _boardConfigurations.value.toMutableMap()
        val emptyConfig = BoardConfiguration(boardId, emptyMap(), emptyMap())
        configurations[boardId] = emptyConfig
        saveAllConfigurations(configurations)
    }
}
