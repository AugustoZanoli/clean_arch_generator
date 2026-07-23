package com.cag.core

class NameVariants(rawInput: String) {

    private val words: List<String> = splitWords(rawInput)

    val pascal: String = words.joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    val camel: String = pascal.replaceFirstChar(Char::lowercaseChar)
    val upperSnake: String = words.joinToString("_") { it.uppercase() }
    val snake: String = words.joinToString("_") { it.lowercase() }
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
