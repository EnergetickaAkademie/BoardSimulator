package eu.swpelc.boardsimulator.model

enum class Source {
    PHOTOVOLTAIC,
    WIND,
    NUCLEAR,
    GAS,
    HYDRO,
    HYDRO_STORAGE,
    COAL,
    BATTERY;

    override fun toString(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
