package com.barboza.bingomanager.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.barboza.bingomanager.data.BingoRepository
import com.barboza.bingomanager.model.BingoCard
import com.barboza.bingomanager.model.BingoRules
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.model.GameMode
import com.barboza.bingomanager.model.GameModeType
import com.barboza.bingomanager.model.GameSession
import com.barboza.bingomanager.model.builtInGameModes
import java.util.UUID

sealed interface AppScreen {
    data object Home : AppScreen
    data class CardEditor(
        val cardId: String? = null,
        val templateId: String? = null,
        val importedNumbers: List<Int?>? = null,
    ) : AppScreen
    data object TemplateDesigner : AppScreen
    data object Manage : AppScreen
    data object CustomModeEditor : AppScreen
    data object PhotoImport : AppScreen
    data object Game : AppScreen
}

/**
 * Controlador MVC: recibe acciones de las vistas, valida las reglas y coordina
 * los modelos con el repositorio local.
 */
class BingoController(private val repository: BingoRepository) {
    private val storedRange = repository.loadLastRange()

    var currentScreen: AppScreen by mutableStateOf(AppScreen.Home)
        private set
    var templates: List<BingoTemplate> by mutableStateOf(repository.loadTemplates())
        private set
    var cards: List<BingoCard> by mutableStateOf(repository.loadCards())
        private set
    var customModes: List<GameMode> by mutableStateOf(repository.loadCustomModes())
        private set
    var gameSession: GameSession? by mutableStateOf(null)
        private set
    var lastMinimumNumber: Int by mutableStateOf(storedRange.first)
        private set
    var lastMaximumNumber: Int by mutableStateOf(storedRange.last)
        private set

    fun navigate(screen: AppScreen) {
        currentScreen = screen
    }

    fun template(id: String?): BingoTemplate? = templates.firstOrNull { it.id == id }
    fun card(id: String?): BingoCard? = cards.firstOrNull { it.id == id }

    fun modesFor(templateId: String): List<GameMode> =
        builtInGameModes() + customModes.filter { it.templateId == templateId }

    fun saveCard(card: BingoCard): String? {
        val template = template(card.templateId) ?: return "El tipo de cartón ya no existe."
        if (card.name.isBlank()) return "Escribe un nombre para identificar el cartón."
        if (card.numbers.size != template.cells.size) return "El cartón no coincide con su plantilla."
        val required = template.cells.indices.filter { template.cells[it] == CellKind.NUMBER }
        if (required.any { card.numbers[it] == null }) return "Completa todas las casillas numéricas."
        if (required.any { (card.numbers[it] ?: 0) <= 0 }) return "Los números deben ser mayores que cero."
        val numbers = required.mapNotNull(card.numbers::getOrNull)
        if (numbers.distinct().size != numbers.size) return "No se permiten números repetidos en un mismo cartón."

        cards = cards.filterNot { it.id == card.id } + card.copy(
            name = card.name.trim(),
            numbers = card.numbers.mapIndexed { index, number ->
                if (template.cells[index] == CellKind.NUMBER) number else null
            },
        )
        repository.saveCards(cards)
        currentScreen = AppScreen.Home
        return null
    }

    fun deleteCard(cardId: String) {
        cards = cards.filterNot { it.id == cardId }
        repository.saveCards(cards)
    }

    fun createTemplate(name: String, rows: Int, columns: Int, cells: List<CellKind>): String? {
        if (name.isBlank()) return "Escribe un nombre para este tipo de cartón."
        if (rows !in 1..20 || columns !in 1..20 || cells.size != rows * columns) {
            return "El tamaño debe estar entre 1 × 1 y 20 × 20."
        }
        if (cells.none { it == CellKind.NUMBER }) return "Activa al menos una casilla numérica."
        if (templates.any { it.name.equals(name.trim(), ignoreCase = true) }) {
            return "Ya existe un tipo de cartón con ese nombre."
        }
        val template = BingoTemplate(UUID.randomUUID().toString(), name.trim(), rows, columns, cells)
        templates = templates + template
        repository.saveTemplates(templates)
        currentScreen = AppScreen.CardEditor(templateId = template.id)
        return null
    }

    fun deleteTemplate(templateId: String): String? {
        if (templateId == BingoTemplate.CLASSIC_ID) return "La plantilla tradicional no se puede eliminar."
        if (cards.any { it.templateId == templateId }) return "Elimina primero los cartones que usan esta plantilla."
        if (customModes.any { it.templateId == templateId }) return "Elimina primero los modos que usan esta plantilla."
        templates = templates.filterNot { it.id == templateId }
        repository.saveTemplates(templates)
        return null
    }

    fun createCustomMode(name: String, templateId: String, selectedCells: Set<Int>): String? {
        val template = template(templateId) ?: return "Selecciona un tipo de cartón válido."
        if (name.isBlank()) return "Escribe un nombre para el modo."
        val validCells = selectedCells.filterTo(mutableSetOf()) { index ->
            template.cells.getOrNull(index) != CellKind.DISABLED
        }
        if (validCells.isEmpty()) return "Selecciona al menos una casilla ganadora."
        if (customModes.any { it.name.equals(name.trim(), ignoreCase = true) && it.templateId == templateId }) {
            return "Ya existe un modo con ese nombre para esta plantilla."
        }
        customModes = customModes + GameMode(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = GameModeType.CUSTOM,
            templateId = templateId,
            selectedCells = validCells,
        )
        repository.saveCustomModes(customModes)
        currentScreen = AppScreen.Manage
        return null
    }

    fun deleteCustomMode(modeId: String) {
        customModes = customModes.filterNot { it.id == modeId }
        repository.saveCustomModes(customModes)
    }

    fun startGame(templateId: String, modeId: String, minimum: Int, maximum: Int): String? {
        val template = template(templateId) ?: return "Selecciona un tipo de cartón."
        if (minimum <= 0 || maximum < minimum) return "Revisa el rango de números."
        if (maximum - minimum > 999) return "El rango puede contener como máximo 1.000 números."
        val selectedCards = cards.filter { it.templateId == templateId }
        if (selectedCards.isEmpty()) return "Agrega al menos un cartón de tipo ${template.name}."
        val mode = modesFor(templateId).firstOrNull { it.id == modeId }
            ?: return "Selecciona un modo de juego compatible."
        val outsideRange = selectedCards.firstOrNull { card ->
            card.numbers.filterNotNull().any { it !in minimum..maximum }
        }
        if (outsideRange != null) {
            return "El cartón “${outsideRange.name}” contiene números fuera del rango $minimum–$maximum."
        }

        gameSession = GameSession(
            templateId = templateId,
            mode = mode,
            minimumNumber = minimum,
            maximumNumber = maximum,
            cardIds = selectedCards.map(BingoCard::id),
        )
        lastMinimumNumber = minimum
        lastMaximumNumber = maximum
        repository.saveLastRange(minimum, maximum)
        currentScreen = AppScreen.Game
        return null
    }

    fun changeGameMode(modeId: String) {
        val session = gameSession ?: return
        val mode = modesFor(session.templateId).firstOrNull { it.id == modeId } ?: return
        gameSession = session.copy(mode = mode)
    }

    fun toggleCalledNumber(number: Int) {
        val session = gameSession ?: return
        val nowCalled = if (number in session.calledNumbers) {
            session.calledNumbers - number
        } else {
            session.calledNumbers + number
        }
        gameSession = session.copy(
            calledNumbers = nowCalled,
            lastCalledNumber = if (number in nowCalled) number else nowCalled.lastOrNull(),
        )
    }

    fun markCalledNumber(number: Int) {
        val session = gameSession ?: return
        if (number !in session.calledNumbers) toggleCalledNumber(number)
    }

    fun resetGame() {
        gameSession = gameSession?.copy(calledNumbers = emptySet(), lastCalledNumber = null)
    }

    fun finishGame() {
        gameSession = null
        currentScreen = AppScreen.Home
    }

    fun gameCards(): List<BingoCard> {
        val ids = gameSession?.cardIds?.toSet().orEmpty()
        return cards.filter { it.id in ids }
    }

    fun winners(): List<BingoCard> {
        val session = gameSession ?: return emptyList()
        val template = template(session.templateId) ?: return emptyList()
        return gameCards().filter { card ->
            BingoRules.isWinner(card, template, session.mode, session.calledNumbers)
        }
    }
}
