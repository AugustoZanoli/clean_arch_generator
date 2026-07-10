package com.cag.core

import com.cag.config.CagConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

/** Um slot já resolvido contra a árvore de diretórios real do projeto. */
data class ResolvedSlot(
    val slot: SlotConfig,
    /** Package do arquivo, derivado do caminho relativo ao source root. */
    val packageName: String,
    /** Caminho do diretório relativo ao source root, preservando o case das pastas existentes. */
    val relativeDir: String,
    /** Diretório existente mais profundo na cadeia (ao menos o source root). */
    val existingBaseDir: VirtualFile,
    /** Segmentos ainda inexistentes, a serem criados sob [existingBaseDir]. */
    val segmentsToCreate: List<String>,
)

/** Resultado da resolução da arquitetura para o projeto/módulo atual. */
data class ResolvedArchitecture(
    val sourceRoot: VirtualFile,
    val basePackage: String,
    val slots: List<ResolvedSlot>,
)

/**
 * Resolve os slots do [CagConfig] contra o(s) source root(s) reais do projeto:
 * descobre o source root, deriva o base package e mapeia cada pasta da arquitetura
 * para um caminho concreto (existente ou a criar), tolerando diferenças de maiúsc./minúsc.
 */
class ArchitectureResolver(private val project: Project) {

    /**
     * @param context arquivo/diretório de referência (ex.: seleção no Project View) usado para
     *                escolher o source root/módulo mais apropriado; pode ser nulo.
     */
    fun resolve(config: CagConfig, context: VirtualFile? = null): ResolvedArchitecture? {
        val sourceRoot = pickSourceRoot(context) ?: return null

        val archRelPath = architectureRelPath(config, sourceRoot)
        val basePackage = archRelPath.toPackage()

        val slots = config.slots.map { slot ->
            val relSegments = (archRelPath.splitPath() + slot.folder.splitPath())
            resolveSlot(slot, sourceRoot, relSegments)
        }
        return ResolvedArchitecture(sourceRoot, basePackage, slots)
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

    /** Caminho da raiz da arquitetura relativo ao source root (config explícito ou auto-detecção). */
    private fun architectureRelPath(config: CagConfig, sourceRoot: VirtualFile): String {
        val configured = config.architectureRoot.trim().trim('/')
        if (configured.isNotEmpty()) return configured

        val topFolders = config.slots.map { it.folder.splitPath().firstOrNull() ?: "" }
            .filter { it.isNotEmpty() }.toSet()
        val detected = walkDownToArchitecture(sourceRoot, topFolders)
        return relativePath(sourceRoot, detected)
    }

    /**
     * Desce por cadeias de diretório único (ex.: com/empresa/app) até achar um diretório que
     * contenha alguma das pastas de topo da arquitetura; caso não ache, retorna o ponto onde parou.
     */
    private fun walkDownToArchitecture(sourceRoot: VirtualFile, topFolders: Set<String>): VirtualFile {
        var dir = sourceRoot
        while (true) {
            val children = dir.children?.filter { it.isDirectory } ?: emptyList()
            if (children.any { child -> topFolders.any { it.equals(child.name, ignoreCase = true) } }) return dir
            val subdirs = children.filterNot { it.name.startsWith(".") }
            if (subdirs.size == 1) dir = subdirs[0] else return dir
        }
    }

    private fun pickSourceRoot(context: VirtualFile?): VirtualFile? {
        val rootManager = ProjectRootManager.getInstance(project)
        val sourceRoots = rootManager.contentSourceRoots.toList()

        // Prioriza um source root que seja ancestral do contexto selecionado.
        if (context != null) {
            sourceRoots.firstOrNull { isAncestor(it, context) }?.let { return it }
        }
        // Prefere source de produção (evita pastas de teste/gerado).
        sourceRoots.firstOrNull { !it.path.contains("/test") && !it.path.contains("generated") }?.let { return it }
        sourceRoots.firstOrNull()?.let { return it }

        // Fallback: raiz de conteúdo ou base do projeto.
        return rootManager.contentRoots.firstOrNull()
    }

    private fun isAncestor(ancestor: VirtualFile, child: VirtualFile): Boolean {
        var cur: VirtualFile? = child
        while (cur != null) {
            if (cur == ancestor) return true
            cur = cur.parent
        }
        return false
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
