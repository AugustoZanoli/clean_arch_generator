package com.cag.core

import com.cag.config.CagConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/** Um arquivo planejado para geração, com tudo que o preview e o gerador precisam. */
data class PlannedFile(
    val slot: SlotConfig,
    /** Nome do arquivo com extensão, ex.: "AssinanteDto.java". */
    val fileName: String,
    val packageName: String,
    /** Caminho relativo ao source root, para exibição, ex.: "com/perfil/app/domain/models/PerfilDto.java". */
    val relativePath: String,
    val content: String,
    val existingBaseDir: VirtualFile,
    val segmentsToCreate: List<String>,
    /** Se já existe no disco (será pulado na geração). */
    val alreadyExists: Boolean,
    /** Marcado no preview (respeita [SlotConfig.enabledByDefault]; desabilitado se já existe). */
    var enabled: Boolean,
)

/** Plano completo de geração de uma feature. */
data class FeaturePlan(
    val featureName: String,
    val sourceRoot: VirtualFile,
    val files: List<PlannedFile>,
)

/**
 * Constrói o [FeaturePlan] a partir do nome da feature, do [CagConfig] e da arquitetura resolvida.
 * Faz duas passagens: primeiro calcula o nome de classe/package de cada slot (para montar as
 * variáveis de referência cruzada `{fqn.<id>}`), depois renderiza o conteúdo de cada template.
 */
class PlanBuilder(private val project: Project) {

    fun build(rawFeatureName: String, config: CagConfig, arch: ResolvedArchitecture): FeaturePlan {
        val name = NameVariants(rawFeatureName)

        // Passagem 1: nome de classe e package de cada slot + variáveis de referência cruzada.
        val classNames = HashMap<String, String>()
        val crossRefVars = HashMap<String, String>()
        arch.slots.forEach { resolved ->
            val className = TemplateEngine.render(resolved.slot.fileName, nameVars(name, arch.basePackage))
            classNames[resolved.slot.id] = className
            val id = resolved.slot.id
            crossRefVars["class.$id"] = className
            crossRefVars["package.$id"] = resolved.packageName
            crossRefVars["fqn.$id"] = fqn(resolved.packageName, className)
        }

        // Passagem 2: renderiza cada arquivo.
        val files = arch.slots.map { resolved ->
            val className = classNames.getValue(resolved.slot.id)
            val fileName = "$className.${resolved.slot.extension}"
            val vars = nameVars(name, arch.basePackage) + crossRefVars + mapOf("package" to resolved.packageName)

            val template = TemplateLoader.load(project, resolved.slot.template, resolved.slot.extension)
            val content = TemplateEngine.render(template, vars)

            val exists = fileExists(resolved.existingBaseDir, resolved.segmentsToCreate, fileName)
            PlannedFile(
                slot = resolved.slot,
                fileName = fileName,
                packageName = resolved.packageName,
                relativePath = listOf(resolved.relativeDir, fileName).filter { it.isNotEmpty() }.joinToString("/"),
                content = content,
                existingBaseDir = resolved.existingBaseDir,
                segmentsToCreate = resolved.segmentsToCreate,
                alreadyExists = exists,
                enabled = resolved.slot.enabledByDefault && !exists,
            )
        }
        return FeaturePlan(name.pascal, arch.sourceRoot, files)
    }

    private fun nameVars(name: NameVariants, basePackage: String): Map<String, String> = mapOf(
        "Feature" to name.pascal,
        "feature" to name.camel,
        "FEATURE" to name.upperSnake,
        "feature_snake" to name.snake,
        "feature-kebab" to name.kebab,
        "basePackage" to basePackage,
    )

    private fun fqn(pkg: String, className: String): String =
        if (pkg.isEmpty()) className else "$pkg.$className"

    /** Existe só se a cadeia de diretórios já existe (nada a criar) e o arquivo está lá. */
    private fun fileExists(baseDir: VirtualFile, segmentsToCreate: List<String>, fileName: String): Boolean {
        if (segmentsToCreate.isNotEmpty()) return false
        return baseDir.findChild(fileName)?.exists() == true
    }
}
