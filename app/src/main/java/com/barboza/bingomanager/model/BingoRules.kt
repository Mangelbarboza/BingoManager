package com.barboza.bingomanager.model

object BingoRules {
    fun isWinner(
        card: BingoCard,
        template: BingoTemplate,
        mode: GameMode,
        calledNumbers: Set<Int>,
    ): Boolean {
        if (card.templateId != template.id || card.numbers.size != template.cells.size) return false

        fun active(index: Int) = template.cells.getOrNull(index) != CellKind.DISABLED
        fun marked(index: Int): Boolean = when (template.cells.getOrNull(index)) {
            CellKind.FREE -> true
            CellKind.NUMBER -> card.numbers.getOrNull(index)?.let(calledNumbers::contains) == true
            else -> false
        }
        fun completed(indices: Collection<Int>): Boolean {
            val playable = indices.filter(::active)
            return playable.isNotEmpty() && playable.all(::marked)
        }

        val rows = (0 until template.rows).map { row ->
            (0 until template.columns).map { column -> row * template.columns + column }
        }
        val columns = (0 until template.columns).map { column ->
            (0 until template.rows).map { row -> row * template.columns + column }
        }
        val mainDiagonal = (0 until minOf(template.rows, template.columns)).map { it * template.columns + it }
        val antiDiagonal = (0 until minOf(template.rows, template.columns)).map {
            it * template.columns + (template.columns - 1 - it)
        }
        val corners = listOf(
            0,
            template.columns - 1,
            (template.rows - 1) * template.columns,
            template.rows * template.columns - 1,
        ).distinct().filter(::active)
        val boundary = template.cells.indices.filter { index ->
            val row = index / template.columns
            val column = index % template.columns
            active(index) && (row == 0 || row == template.rows - 1 || column == 0 || column == template.columns - 1)
        }

        return when (mode.type) {
            GameModeType.FULL -> completed(template.cells.indices.toList())
            GameModeType.FOUR_CORNERS -> corners.size == 4 && corners.all(::marked)
            GameModeType.THREE_CORNERS -> corners.count(::marked) >= 3
            GameModeType.TWO_CORNERS -> corners.count(::marked) >= 2
            GameModeType.ANY_ROW -> rows.any(::completed)
            GameModeType.ANY_COLUMN -> columns.any(::completed)
            GameModeType.DIAGONAL -> completed(mainDiagonal) || completed(antiDiagonal)
            GameModeType.X_SHAPE -> completed((mainDiagonal + antiDiagonal).distinct())
            GameModeType.L_SHAPE -> {
                val top = rows.first()
                val bottom = rows.last()
                val left = columns.first()
                val right = columns.last()
                listOf(left + top, left + bottom, right + top, right + bottom)
                    .any { completed(it.distinct()) }
            }
            GameModeType.T_SHAPE -> {
                val middleRow = rows[template.rows / 2]
                val middleColumn = columns[template.columns / 2]
                listOf(
                    rows.first() + middleColumn,
                    rows.last() + middleColumn,
                    columns.first() + middleRow,
                    columns.last() + middleRow,
                ).any { completed(it.distinct()) }
            }
            GameModeType.FRAME -> completed(boundary)
            GameModeType.QUICK_COUNT -> template.cells.indices.count { index ->
                template.cells[index] == CellKind.NUMBER && marked(index)
            } >= mode.requiredCount
            GameModeType.CUSTOM -> mode.templateId == template.id && completed(mode.selectedCells)
        }
    }

    fun markedIndices(card: BingoCard, template: BingoTemplate, calledNumbers: Set<Int>): Set<Int> =
        template.cells.indices.filterTo(mutableSetOf()) { index ->
            template.cells[index] == CellKind.FREE ||
                (template.cells[index] == CellKind.NUMBER &&
                    card.numbers.getOrNull(index)?.let(calledNumbers::contains) == true)
        }
}
