package com.cag.core

/**
 * Motor de template minimalista: substitui placeholders `{chave}` pelos valores fornecidos.
 * Placeholders sem valor correspondente são mantidos como estão (facilita depurar templates).
 *
 * Chaves podem conter letras, dígitos, `.`, `_` e `-` (ex.: `{Feature}`, `{fqn.useCase}`).
 */
object TemplateEngine {

    private val PLACEHOLDER = Regex("\\{([A-Za-z0-9_.\\-]+)}")

    fun render(template: String, variables: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match ->
            variables[match.groupValues[1]] ?: match.value
        }
}
