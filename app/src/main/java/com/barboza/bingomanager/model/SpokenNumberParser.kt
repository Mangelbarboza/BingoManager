package com.barboza.bingomanager.model

import java.text.Normalizer
import java.util.Locale

object SpokenNumberParser {
    private val smallNumbers = mapOf(
        "cero" to 0, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
        "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14,
        "quince" to 15, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18,
        "diecinueve" to 19, "veinte" to 20, "veintiuno" to 21, "veintidos" to 22,
        "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
        "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
    )
    private val tens = mapOf(
        "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50, "sesenta" to 60,
        "setenta" to 70, "ochenta" to 80, "noventa" to 90,
    )
    private val hundreds = mapOf(
        "cien" to 100, "ciento" to 100, "doscientos" to 200, "trescientos" to 300,
        "cuatrocientos" to 400, "quinientos" to 500, "seiscientos" to 600,
        "setecientos" to 700, "ochocientos" to 800, "novecientos" to 900,
    )

    fun parse(text: String): Int? {
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace('-', ' ')
        Regex("(?<!\\d)\\d{1,4}(?!\\d)").find(normalized)?.value?.toIntOrNull()?.let { return it }

        val tokens = normalized.split(Regex("[^a-z]+"))
            .filter { it.isNotBlank() && it !in setOf("y", "numero", "el", "la", "salio", "sale", "es") }
        if (tokens.isEmpty()) return null
        var total = 0
        var found = false
        tokens.forEach { token ->
            val value = hundreds[token] ?: tens[token] ?: smallNumbers[token]
            if (value != null) {
                total += value
                found = true
            }
        }
        return total.takeIf { found && it in 0..9999 }
    }
}
