package com.cag.core

import com.cag.config.CagConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

data class PlannedFile(
    val slot: SlotConfig,
    val fileName: String,
    val packageName: String,
    val relativePath: String,
    val content: String,
    val existingBaseDir: VirtualFile,
    val segmentsToCreate: List<String>,
    val alreadyExists: Boolean,
    var enabled: Boolean,
)

data class FeaturePlan(
    val featureName: String,
    val sourceRoot: VirtualFile,
    val files: List<PlannedFile>,
)

class PlanBuilder(private val project: Project) {

    fun build(rawFeatureName: String, config: CagConfig, arch: ResolvedArchitecture): FeaturePlan {
        val name = NameVariants(rawFeatureName)
        val baseVars = nameVars(name, arch.basePackage)

        val classNames = HashMap<String, String>()
        val crossRefVars = HashMap<String, String>()
        arch.slots.forEach { resolved ->
            val className = TemplateEngine.render(resolved.slot.fileName, baseVars)
            classNames[resolved.slot.id] = className
            val id = resolved.slot.id
            crossRefVars["class.$id"] = className
            crossRefVars["package.$id"] = resolved.packageName
            crossRefVars["fqn.$id"] = fqn(resolved.packageName, className)
        }

        val files = arch.slots.map { resolved ->
            val className = classNames.getValue(resolved.slot.id)
            val fileName = "$className.${resolved.slot.extension}"
            val vars = baseVars + crossRefVars + mapOf("package" to resolved.packageName)

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

    private fun nameVars(name: NameVariants, basePackage: String): Map<String, String> = buildMap {
        put("Feature", name.pascal)
        put("feature", name.camel)
        put("FEATURE", name.upperSnake)
        put("feature_snake", name.snake)
        put("feature-kebab", name.kebab)
        put("basePackage", basePackage)
    }

    private fun fqn(pkg: String, className: String): String =
        if (pkg.isEmpty()) className else "$pkg.$className"

    private fun fileExists(baseDir: VirtualFile, segmentsToCreate: List<String>, fileName: String): Boolean {
        if (segmentsToCreate.isNotEmpty()) return false
        return baseDir.findChild(fileName)?.exists() == true
    }
}
