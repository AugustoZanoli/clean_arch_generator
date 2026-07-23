package com.cag.core

import com.cag.config.CagConfig
import com.intellij.openapi.project.Project
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Carrega templates com cache para evitar leituras repetidas do disco.
 * Prioridade: projeto > embutido > fallback em código.
 */
object TemplateLoader {

    private val bundledCache = HashMap<String, String>()

    fun load(project: Project, templateName: String, extension: String): String {
        projectTemplate(project, templateName)?.let { return it }
        bundledTemplate(templateName)?.let { return it }
        return fallback(extension)
    }

    private fun projectTemplate(project: Project, templateName: String): String? {
        val base = project.basePath ?: return null
        val path: Path = Path.of(base, CagConfig.CONFIG_DIR, CagConfig.TEMPLATES_DIR, "$templateName.template")
        return if (path.exists()) runCatching { path.readText() }.getOrNull() else null
    }

    private fun bundledTemplate(templateName: String): String? =
        bundledCache.getOrPut(templateName) {
            javaClass.getResourceAsStream("/defaults/templates/$templateName.template")
                ?.bufferedReader()?.use { it.readText() } ?: return null
        }

    private fun fallback(extension: String): String = when (extension.lowercase()) {
        "kt" -> "package {package}\n\nclass {Feature} {\n}\n"
        else -> "package {package};\n\npublic class {Feature} {\n}\n"
    }
}
