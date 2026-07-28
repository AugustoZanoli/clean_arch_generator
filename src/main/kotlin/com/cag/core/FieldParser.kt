package com.cag.core

import com.intellij.openapi.vfs.VirtualFile

data class FieldInfo(
    val name: String,
    val type: String,
) {
    val getter: String get() = "get${name.replaceFirstChar { it.uppercase() }}()"
    val setter: String get() = "set${name.replaceFirstChar { it.uppercase() }}"
}

object FieldParser {

    private val FIELD_REGEX = Regex(
        """(?:private|protected|public)\s+([\w<>,\s?]+?)\s+(\w+)\s*[;=]"""
    )

    private val KOTLIN_FIELD_REGEX = Regex(
        """(?:val|var)\s+(\w+)\s*:\s*([\w<>,\s?]+)"""
    )

    fun parse(file: VirtualFile): List<FieldInfo> {
        val content = runCatching { String(file.contentsToByteArray()) }.getOrNull() ?: return emptyList()
        return parseContent(content)
    }

    fun parseContent(content: String): List<FieldInfo> {
        val javaFields = FIELD_REGEX.findAll(content).map { match ->
            FieldInfo(
                name = match.groupValues[2].trim(),
                type = match.groupValues[1].trim(),
            )
        }
        val kotlinFields = KOTLIN_FIELD_REGEX.findAll(content).map { match ->
            FieldInfo(
                name = match.groupValues[1].trim(),
                type = match.groupValues[2].trim(),
            )
        }
        return (javaFields + kotlinFields).toList()
    }

    fun renderDeclarations(fields: List<FieldInfo>): String =
        fields.joinToString("\n") { "private ${it.type} ${it.name};" }

    fun renderAssignments(fields: List<FieldInfo>, source: String = "source", target: String = "result"): String =
        fields.joinToString("\n") { "        ${target}.${it.setter}(${source}.${it.getter});" }
}
