package eu.swpelc.boardsimulator.model

data class ServerSettings(
    val serverIp: String = "",
    val lecturerUsername: String = "lecturer1",
    val lecturerPassword: String = "lecturer123",
    val boardUsernames: String = "board1, board2, board3",
    val refreshInterval: Long = 10000L // Default 10 seconds in milliseconds
)

data class LoginResponse(
    val group_id: String,
    val token: String,
    val user_type: String,
    val username: String
)

data class SimulationDump(
    val groups: Map<String, GroupData>,
    val summary: SummaryData,
    val timestamp: Double
)

data class GroupData(
    val boards: Map<String, BoardData>,
    val consumption_modifiers: Map<String, Double>,
    val game_status: GameStatus,
    val group_id: String,
    val powerplant_ranges: Map<String, PowerplantRange>?,
    val production_coefficients: Map<String, Double>
)

data class BoardData(
    val board_id: String,
    val connected_consumption: List<Int>,
    val connected_production: List<Int>,
    val consumption_history: List<Double>,
    val current_consumption: Double,
    val current_production: Double,
    val history_length: HistoryLength,
    val last_updated: Double,
    val production_history: List<Double>
)

data class HistoryLength(
    val consumption: Int,
    val production: Int
)

data class PowerplantRange(
    val max: Double,
    val min: Double
)

data class GameStatus(
    val active: Boolean,
    val current_round: Int,
    val game_finished: Boolean,
    val round_type: Int,
    val scenario: String,
    val total_rounds: Int
)

data class SummaryData(
    val active_games: Int,
    val total_boards: Int,
    val total_groups: Int
)
