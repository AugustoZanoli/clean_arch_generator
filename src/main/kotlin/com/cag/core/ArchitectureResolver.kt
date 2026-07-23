package com.cag.core

import com.cag.config.CagConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

data class ModuleItem(
    val file: VirtualFile,
    val displayName: String,
)

data class ResolvedSlot(
    val slot: SlotConfig,
    val packageName: String,
    val relativeDir: String,
    val existingBaseDir: VirtualFile,
    val segmentsToCreate: List<String>,
)

data class ResolvedArchitecture(
    val sourceRoot: VirtualFile,
    val basePackage: String,
    val slots: List<ResolvedSlot>,
)

class ArchitectureResolver(private val project: Project) {

    companion object {
        private val STANDARD_PACKAGES = setOf(
            "com", "org", "net", "io", "br", "java", "javax", "kotlin",
        )
    }

    fun resolve(
        config: CagConfig,
        context: VirtualFile? = null,
        selectedModule: VirtualFile? = null,
        featureName: String = "",
    ): ResolvedArchitecture? {
        val sourceRoot = findSourceRootForModule(selectedModule) ?: pickSourceRoot(context) ?: return null

        val baseDir = if (selectedModule != null && selectedModule != sourceRoot) {
            selectedModule
        } else {
            val archRelPath = architectureRelPath(config, sourceRoot)
            if (archRelPath.isNotEmpty()) {
                sourceRoot.findFileByRelativePath(archRelPath) ?: sourceRoot
            } else {
                sourceRoot
            }
        }

        val basePackage = relativePath(sourceRoot, baseDir).toPackage()

        val slots = config.slots.map { slot ->
            val resolvedFolder = slot.folder.replace("{feature}", featureName)
            val relSegments = relativePath(sourceRoot, baseDir).splitPath() + resolvedFolder.splitPath()
            resolveSlot(slot, sourceRoot, relSegments)
        }
        return ResolvedArchitecture(sourceRoot, basePackage, slots)
    }

    private fun findSourceRootForModule(module: VirtualFile?): VirtualFile? {
        if (module == null) return null
        val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
        if (sourceRoots.any { it.path == module.path }) return module
        return sourceRoots.firstOrNull { isAncestor(it, module) }
    }

    private fun resolveSlot(
        slot: SlotConfig,
        sourceRoot: VirtualFile,
        relSegments: List<String>,
    ): ResolvedSlot {
        var existing = sourceRoot
        val resolvedNames = mutableListOf<String>()
        val toCreate = mutableListOf<String>()
        var stillExisting = true

        for (segment in relSegments) {
            if (stillExisting) {
                val child = existing.children?.firstOrNull { it.isDirectory && it.name.equals(segment, ignoreCase = true) }
                if (child != null) {
                    existing = child
                    resolvedNames += child.name
                    continue
                }
                stillExisting = false
            }
            resolvedNames += segment
            toCreate += segment
        }

        val relativeDir = resolvedNames.joinToString("/")
        return ResolvedSlot(
            slot = slot,
            packageName = resolvedNames.joinToString("."),
            relativeDir = relativeDir,
            existingBaseDir = existing,
            segmentsToCreate = toCreate,
        )
    }

    private fun architectureRelPath(config: CagConfig, sourceRoot: VirtualFile): String {
        val configured = config.architectureRoot.trim().trim('/')
        if (configured.isNotEmpty()) return configured

        val topFolders = config.slots.mapNotNull { it.folder.splitPath().firstOrNull() }.toSet()
        val detected = walkDownToArchitecture(sourceRoot, topFolders)
        return relativePath(sourceRoot, detected)
    }

    private fun walkDownToArchitecture(sourceRoot: VirtualFile, topFolders: Set<String>): VirtualFile {
        var dir = sourceRoot
        while (true) {
            val children = dir.children?.filter { it.isDirectory } ?: emptyList()
            if (children.any { child -> topFolders.any { it.equals(child.name, ignoreCase = true) } }) return dir
            val subdirs = children.filterNot { it.name.startsWith(".") }
            if (subdirs.size == 1) dir = subdirs[0] else return dir
        }
    }

    fun availableModules(): List<ModuleItem> {
        val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots
            .filter { !it.path.contains("/test") && !it.path.contains("generated") }

        val modules = mutableListOf<VirtualFile>()

        for (root in sourceRoots) {
            var dir = root
            while (true) {
                val children = dir.children?.filter { it.isDirectory && !it.name.startsWith(".") } ?: emptyList()
                if (children.size == 1) {
                    dir = children[0]
                } else {
                    val moduleDirs = children.filter { it.name.lowercase() !in STANDARD_PACKAGES }
                    if (moduleDirs.isNotEmpty()) {
                        modules.addAll(moduleDirs.sortedBy { it.name })
                    } else {
                        modules.add(root)
                    }
                    break
                }
            }
        }

        if (modules.isEmpty()) {
            return sourceRoots.sortedBy { it.name }.map { ModuleItem(it, it.name) }
        }

        val distinctModules = modules.distinctBy { it.path }.sortedBy { it.name }
        val hasDuplicates = distinctModules.groupBy { it.name.lowercase() }.any { it.value.size > 1 }

        return if (hasDuplicates) {
            distinctModules.map { module ->
                val parentName = findParentModuleName(module)
                val displayName = if (parentName != null) "$parentName/${module.name}" else module.name
                ModuleItem(module, displayName)
            }.sortedBy { it.displayName }
        } else {
            distinctModules.map { ModuleItem(it, it.name) }.sortedBy { it.displayName }
        }
    }

    private fun findParentModuleName(module: VirtualFile): String? {
        val parent = module.parent ?: return null
        val sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.map { it.path }
        if (sourceRoots.any { it == parent.path }) return null
        if (parent.name.lowercase() in STANDARD_PACKAGES) return null
        return parent.name
    }

    private fun pickSourceRoot(context: VirtualFile?): VirtualFile? {
        val rootManager = ProjectRootManager.getInstance(project)
        val sourceRoots = rootManager.contentSourceRoots.toList()

        if (context != null) {
            sourceRoots.firstOrNull { isAncestor(it, context) }?.let { return it }
        }
        sourceRoots.firstOrNull { !it.path.contains("/test") && !it.path.contains("generated") }?.let { return it }
        sourceRoots.firstOrNull()?.let { return it }
        return rootManager.contentRoots.firstOrNull()
    }

    private fun relativePath(root: VirtualFile, descendant: VirtualFile): String {
        if (root == descendant) return ""
        val rootPath = root.path.trimEnd('/')
        val childPath = descendant.path
        return if (childPath.startsWith("$rootPath/")) childPath.substring(rootPath.length + 1) else ""
    }

    private fun String.splitPath(): List<String> =
        split('/').map { it.trim() }.filter { it.isNotEmpty() }

    private fun String.toPackage(): String =
        splitPath().joinToString(".")
}
