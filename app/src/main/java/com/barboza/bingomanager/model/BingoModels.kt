package com.barboza.bingomanager.model

import java.util.UUID

enum class CellKind { NUMBER, FREE, DISABLED }

data class BingoTemplate(
    val id: String,
    val name: String,
    val rows: Int,
    val columns: Int,
    val cells: List<CellKind>,
) {
    init {
        require(rows in 1..20 && columns in 1..20)
        require(cells.size == rows * columns)
    }

    companion object {
        const val CLASSIC_ID = "classic-5x5"

        fun classic(): BingoTemplate = BingoTemplate(
            id = CLASSIC_ID,
            name = "Tradicional 5 × 5",
            rows = 5,
            columns = 5,
            cells = List(25) { index -> if (index == 12) CellKind.FREE else CellKind.NUMBER },
        )
    }
}

data class BingoCard(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val templateId: String,
    val numbers: List<Int?>,
)

enum class GameModeType {
    FULL,
    FOUR_CORNERS,
    THREE_CORNERS,
    TWO_CORNERS,
    ANY_ROW,
    ANY_COLUMN,
    DIAGONAL,
    X_SHAPE,
    L_SHAPE,
    T_SHAPE,
    FRAME,
    QUICK_COUNT,
    CUSTOM,
}

data class GameMode(
    val id: String,
    val name: String,
    val type: GameModeType,
    val requiredCount: Int = 0,
    val templateId: String? = null,
    val selectedCells: Set<Int> = emptySet(),
)

data class GameSession(
    val templateId: String,
    val mode: GameMode,
    val minimumNumber: Int,
    val maximumNumber: Int,
    val cardIds: List<String>,
    val calledNumbers: Set<Int> = emptySet(),
    val lastCalledNumber: Int? = null,
)

fun builtInGameModes(): List<GameMode> = listOf(
    GameMode("full", "Cartón lleno", GameModeType.FULL),
    GameMode("row", "Una línea horizontal", GameModeType.ANY_ROW),
    GameMode("column", "Una línea vertical", GameModeType.ANY_COLUMN),
    GameMode("diagonal", "Una diagonal", GameModeType.DIAGONAL),
    GameMode("four-corners", "Cuatro esquinas", GameModeType.FOUR_CORNERS),
    GameMode("three-corners", "Tres esquinas", GameModeType.THREE_CORNERS),
    GameMode("two-corners", "Dos esquinas", GameModeType.TWO_CORNERS),
    GameMode("x", "Forma de X", GameModeType.X_SHAPE),
    GameMode("l", "Forma de L", GameModeType.L_SHAPE),
    GameMode("t", "Forma de T", GameModeType.T_SHAPE),
    GameMode("frame", "Marco exterior", GameModeType.FRAME),
    GameMode("quick-1", "Rápido: 1 número", GameModeType.QUICK_COUNT, requiredCount = 1),
    GameMode("quick-2", "Rápido: 2 números", GameModeType.QUICK_COUNT, requiredCount = 2),
    GameMode("quick-3", "Rápido: 3 números", GameModeType.QUICK_COUNT, requiredCount = 3),
)
