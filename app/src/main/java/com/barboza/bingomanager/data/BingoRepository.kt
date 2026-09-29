package com.barboza.bingomanager.data

import android.content.Context
import com.barboza.bingomanager.model.BingoCard
import com.barboza.bingomanager.model.BingoTemplate
import com.barboza.bingomanager.model.CellKind
import com.barboza.bingomanager.model.GameMode
import com.barboza.bingomanager.model.GameModeType
import org.json.JSONArray
import org.json.JSONObject

/** Persistencia local. No usa red, cuentas ni servicios externos. */
class BingoRepository(context: Context) {
    private val preferences = context.getSharedPreferences("bingo_manager_data", Context.MODE_PRIVATE)

    fun loadTemplates(): List<BingoTemplate> {
        val stored = parseArray(preferences.getString(KEY_TEMPLATES, null)) { json ->
            val rows = json.getInt("rows")
            val columns = json.getInt("columns")
            BingoTemplate(
                id = json.getString("id"),
                name = json.getString("name"),
                rows = rows,
                columns = columns,
                cells = json.getJSONArray("cells").toStringList().map { value ->
                    runCatching { CellKind.valueOf(value) }.getOrDefault(CellKind.DISABLED)
                }.take(rows * columns).let { cells ->
                    cells + List((rows * columns - cells.size).coerceAtLeast(0)) { CellKind.DISABLED }
                },
            )
        }
        return listOf(BingoTemplate.classic()) + stored.filterNot { it.id == BingoTemplate.CLASSIC_ID }
    }

    fun saveTemplates(templates: List<BingoTemplate>) {
        val array = JSONArray()
        templates.filterNot { it.id == BingoTemplate.CLASSIC_ID }.forEach { template ->
            array.put(JSONObject().apply {
                put("id", template.id)
                put("name", template.name)
                put("rows", template.rows)
                put("columns", template.columns)
                put("cells", JSONArray(template.cells.map(CellKind::name)))
            })
        }
        preferences.edit().putString(KEY_TEMPLATES, array.toString()).apply()
    }

    fun loadCards(): List<BingoCard> = parseArray(preferences.getString(KEY_CARDS, null)) { json ->
        val numberArray = json.getJSONArray("numbers")
        BingoCard(
            id = json.getString("id"),
            name = json.getString("name"),
            templateId = json.getString("templateId"),
            numbers = List(numberArray.length()) { index ->
                if (numberArray.isNull(index)) null else numberArray.getInt(index)
            },
        )
    }

    fun saveCards(cards: List<BingoCard>) {
        val array = JSONArray()
        cards.forEach { card ->
            array.put(JSONObject().apply {
                put("id", card.id)
                put("name", card.name)
                put("templateId", card.templateId)
                put("numbers", JSONArray().apply {
                    card.numbers.forEach { number -> put(number ?: JSONObject.NULL) }
                })
            })
        }
        preferences.edit().putString(KEY_CARDS, array.toString()).apply()
    }

    fun loadCustomModes(): List<GameMode> = parseArray(preferences.getString(KEY_MODES, null)) { json ->
        GameMode(
            id = json.getString("id"),
            name = json.getString("name"),
            type = GameModeType.CUSTOM,
            templateId = json.getString("templateId"),
            selectedCells = json.getJSONArray("selectedCells").toIntSet(),
        )
    }

    fun saveCustomModes(modes: List<GameMode>) {
        val array = JSONArray()
        modes.filter { it.type == GameModeType.CUSTOM }.forEach { mode ->
            array.put(JSONObject().apply {
                put("id", mode.id)
                put("name", mode.name)
                put("templateId", mode.templateId)
                put("selectedCells", JSONArray(mode.selectedCells.sorted()))
            })
        }
        preferences.edit().putString(KEY_MODES, array.toString()).apply()
    }

    fun loadLastRange(): IntRange {
        val minimum = preferences.getInt(KEY_MINIMUM_NUMBER, 1)
        val maximum = preferences.getInt(KEY_MAXIMUM_NUMBER, 75)
        return if (minimum > 0 && maximum >= minimum) minimum..maximum else 1..75
    }

    fun saveLastRange(minimum: Int, maximum: Int) {
        preferences.edit()
            .putInt(KEY_MINIMUM_NUMBER, minimum)
            .putInt(KEY_MAXIMUM_NUMBER, maximum)
            .apply()
    }

    private fun <T> parseArray(raw: String?, mapper: (JSONObject) -> T): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index -> mapper(array.getJSONObject(index)) }
        }.getOrDefault(emptyList())
    }

    private fun JSONArray.toStringList() = List(length()) { index -> getString(index) }
    private fun JSONArray.toIntSet() = List(length()) { index -> getInt(index) }.toSet()

    private companion object {
        const val KEY_TEMPLATES = "templates"
        const val KEY_CARDS = "cards"
        const val KEY_MODES = "modes"
        const val KEY_MINIMUM_NUMBER = "minimum_number"
        const val KEY_MAXIMUM_NUMBER = "maximum_number"
    }
}
