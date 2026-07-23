package com.cag.core

object TemplateEngine {

    private val PLACEHOLDER = Regex("\\{([A-Za-z0-9_.\\-]+)}")

    fun render(template: String, variables: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match ->
            variables[match.groupValues[1]] ?: match.value
        }
}
