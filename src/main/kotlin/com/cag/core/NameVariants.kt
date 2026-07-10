package com.cag.core

/**
 * Deriva as variações de nome de uma feature a partir da entrada bruta do usuário.
 * Aceita "assinante", "Assinante", "assinante-plano", "assinante_plano", "Assinante Plano"...
 * e normaliza para as convenções usuais.
 */
class NameVariants(rawInput: String) {

    /** Palavras extraídas da entrada (split em espaço, hífen, underscore e camelCase). */
    private val words: List<String> = splitWords(rawInput)

    /** PascalCase, ex.: "AssinantePlano". */
    val pascal: String = words.joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }

    /** camelCase, ex.: "assinantePlano". */
    val camel: String = pascal.replaceFirstChar(Char::lowercaseChar)

    /** UPPER_SNAKE, ex.: "ASSINANTE_PLANO". */
    val upperSnake: String = words.joinToString("_") { it.uppercase() }

    /** lower_snake, ex.: "assinante_plano". */
    val snake: String = words.joinToString("_") { it.lowercase() }

    /** kebab-case, ex.: "assinante-plano". */
    val kebab: String = words.joinToString("-") { it.lowercase() }

    val isValid: Boolean = words.isNotEmpty()

    companion object {
        private val CAMEL_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")

        private fun splitWords(input: String): List<String> =
            input.trim()
                .split(Regex("[\\s_\\-]+"))
                .flatMap { it.split(CAMEL_BOUNDARY) }
                .map { it.filter(Char::isLetterOrDigit) }
                .filter { it.isNotEmpty() }
    }
}
