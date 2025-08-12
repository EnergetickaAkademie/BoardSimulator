package eu.swpelc.boardsimulator.model

data class BoardConfiguration(
    val boardId: String,
    val buildings: Map<String, Int> = emptyMap(), // Building name to quantity
    val powerplants: Map<String, Int> = emptyMap(), // Powerplant name to quantity
    val productionLevels: Map<String, Double> = emptyMap() // Powerplant name to current production level
)

data class BoardBuildingItem(
    val name: String,
    val consumption: Double,
    val quantity: Int
)

data class PowerplantItem(
    val name: String,
    val coefficient: Double,
    val quantity: Int
)
