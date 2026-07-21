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
     * @param selectedModule módulo selecionado pelo usuário (ComboBox); pode ser um source root
     *                       ou um subdiretório dentro do source root (ex.: empresa, fundo).
     * @param featureName nome da feature (snake_case) para substituir {feature} nas pastas.
     */
    fun resolve(config: CagConfig, context: VirtualFile? = null, selectedModule: VirtualFile? = null, featureName: String = ""): ResolvedArchitecture? {
        // Encontra o source root que contém o módulo selecionado
        val sourceRoot = findSourceRootForModule(selectedModule) ?: pickSourceRoot(context) ?: return null

        // Se um módulo foi selecionado e não é o próprio source root, usa ele como base
        val baseDir = if (selectedModule != null && selectedModule != sourceRoot) {
            selectedModule
        } else {
            // Selecione o architecture root apenas quando não há módulo explícito
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
            val relSegments = (relativePath(sourceRoot, baseDir).splitPath() + resolvedFolder.splitPath())
            resolveSlot(slot, sourceRoot, relSegments)
        }
        return ResolvedArchitecture(sourceRoot, basePackage, slots)
    }

    /** Encontra o source root que contém o módulo selecionado. */
    private fun findSourceRootForModule(module: VirtualFile?): VirtualFile? {
        if (module == null) return null
        val rootManager = ProjectRootManager.getInstance(project)
        val sourceRoots = rootManager.contentSourceRoots.toList()

        // Se o módulo é um source root, retorna ele mesmo
        if (sourceRoots.any { it.path == module.path }) return module

        // Senão, encontra o source root ancestral do módulo
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

    /**
     * Retorna os módulos disponíveis para seleção no ComboBox.
     * Para projetos com submódulos (ex.: empresa, fundo dentro de com.dealguard.cessao),
     * retorna os subdiretórios do base package como módulos.
     * Para projetos multi-Maven, retorna os source roots de produção.
     */
    fun availableModules(): List<VirtualFile> {
        val rootManager = ProjectRootManager.getInstance(project)
        val sourceRoots = rootManager.contentSourceRoots.toList()
            .filter { !it.path.contains("/test") && !it.path.contains("generated") }

        // Para cada source root, desce pela cadeia de diretório único e coleta subdiretórios
        // que representam módulos (pastas que NÃO são package padrão como com, org, etc.)
        val modules = mutableListOf<VirtualFile>()
        val standardPackages = setOf("com", "org", "net", "io", "br", "java", "javax", "kotlin")

        for (root in sourceRoots) {
            var dir = root
            // Desce pela cadeia de diretório único até achar múltiplos subdiretórios
            while (true) {
                val children = dir.children?.filter { it.isDirectory && !it.name.startsWith(".") } ?: emptyList()
                if (children.size == 1) {
                    dir = children[0]
                } else {
                    // Achou múltiplos subdiretórios - estes são os módulos
                    val moduleDirs = children.filter { child ->
                        !standardPackages.contains(child.name.lowercase())
                    }
                    if (moduleDirs.isNotEmpty()) {
                        modules.addAll(moduleDirs.sortedBy { it.name })
                    } else {
                        // Se todos são packages padrão, usa o source root
                        modules.add(root)
                    }
                    break
                }
            }
        }

        // Se não encontrou módulos, retorna os source roots como fallback
        if (modules.isEmpty()) {
            return sourceRoots.sortedBy { it.name }
        }

        return modules.distinctBy { it.path }.sortedBy { it.name }
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
