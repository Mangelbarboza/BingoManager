package com.barboza.bingomanager

import com.barboza.bingomanager.model.BingoCard
import com.barboza.bingomanager.model.BingoRules
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.model.GameMode
import com.barboza.bingomanager.model.GameModeType
import com.barboza.bingomanager.model.SpokenNumberParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BingoRulesTest {
    private val template = BingoTemplate.classic()
    private val card = BingoCard(
        name = "Prueba",
        templateId = template.id,
        numbers = List(25) { index -> if (index == 12) null else index + 1 },
    )

    @Test
    fun fullCardRequiresEveryNumberButTreatsFreeSpaceAsMarked() {
        val allNumbers = (1..25).toSet() - 13
        assertTrue(BingoRules.isWinner(card, template, mode(GameModeType.FULL), allNumbers))
        assertFalse(BingoRules.isWinner(card, template, mode(GameModeType.FULL), allNumbers - 25))
    }

    @Test
    fun horizontalLineWinsWhenOneCompleteRowIsCalled() {
        assertTrue(BingoRules.isWinner(card, template, mode(GameModeType.ANY_ROW), (1..5).toSet()))
        assertFalse(BingoRules.isWinner(card, template, mode(GameModeType.ANY_ROW), (1..4).toSet()))
    }

    @Test
    fun cornerAndQuickModesUseTheirOwnRules() {
        val corners = setOf(1, 5, 21, 25)
        assertTrue(BingoRules.isWinner(card, template, mode(GameModeType.FOUR_CORNERS), corners))
        assertTrue(BingoRules.isWinner(card, template, mode(GameModeType.THREE_CORNERS), corners - 25))
        assertTrue(BingoRules.isWinner(card, template, mode(GameModeType.QUICK_COUNT, 2), setOf(2, 8)))
        assertFalse(BingoRules.isWinner(card, template, mode(GameModeType.QUICK_COUNT, 2), setOf(2)))
    }

    @Test
    fun customModeRequiresOnlyItsSelectedShape() {
        val custom = GameMode(
            id = "custom",
            name = "Forma",
            type = GameModeType.CUSTOM,
            templateId = template.id,
            selectedCells = setOf(0, 6, 12),
        )
        assertTrue(BingoRules.isWinner(card, template, custom, setOf(1, 7)))
        assertFalse(BingoRules.isWinner(card, template, custom, setOf(1)))
    }

    @Test
    fun spokenSpanishNumbersAreConvertedForMicrophoneConfirmation() {
        assertTrue(SpokenNumberParser.parse("número setenta y cinco") == 75)
        assertTrue(SpokenNumberParser.parse("veintidós") == 22)
        assertTrue(SpokenNumberParser.parse("salió 49") == 49)
    }

    private fun mode(type: GameModeType, count: Int = 0) =
        GameMode(type.name, type.name, type, requiredCount = count)
}
