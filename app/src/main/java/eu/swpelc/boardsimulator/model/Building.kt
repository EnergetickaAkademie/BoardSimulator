package eu.swpelc.boardsimulator.model

enum class Building {
    CITY_CENTER,
    CITY_CENTER_A,
    CITY_CENTER_B,
    CITY_CENTER_C,
    CITY_CENTER_D,
    CITY_CENTER_E,
    CITY_CENTER_F,
    FACTORY,
    STADIUM,
    HOSPITAL,
    UNIVERSITY,
    AIRPORT,
    SHOPPING_MALL,
    TECHNOLOGY_CENTER,
    FARM,
    LIVING_QUARTER_SMALL,
    LIVING_QUARTER_LARGE,
    SCHOOL;

    override fun toString(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
