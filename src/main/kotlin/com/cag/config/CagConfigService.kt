package com.cag.config

import com.google.gson.GsonBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

@Service(Service.Level.PROJECT)
class CagConfigService(private val project: Project) : Disposable {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var lastModified: Long = 0

    @Volatile
    private var cached: CagConfig? = null

    init {
        configPath()?.let { path ->
            if (path.exists()) lastModified = path.toFile().lastModified()
        }
    }

    fun checkAndReload() {
        val path = configPath() ?: return
        if (!path.exists()) return

        val currentModified = path.toFile().lastModified()
        if (currentModified > lastModified) {
            thisLogger().info("Cag: arquivo de configuração alterado externamente, recarregando...")
            lastModified = currentModified
            cached = null
        }
    }

    fun getConfig(): CagConfig {
        checkAndReload()
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = load()
            cached = loaded
            return loaded
        }
    }

    fun reload() {
        cached = null
    }

    fun save(config: CagConfig) {
        val path = configPath() ?: run {
            thisLogger().warn("Cag: projeto sem basePath; config não pôde ser salva.")
            return
        }
        Files.createDirectories(path.parent)
        Files.writeString(path, gson.toJson(config))
        lastModified = path.toFile().lastModified()
        cached = config.copyDeep()
    }

    private fun load(): CagConfig {
        val path = configPath()
        if (path != null && path.exists()) {
            runCatching { gson.fromJson(path.readText(), CagConfig::class.java) }
                .onSuccess { if (it != null) return normalize(it) }
                .onFailure { thisLogger().warn("Cag: falha ao ler ${path}, usando default.", it) }
        }
        val default = defaultConfig()
        if (path != null) runCatching { save(default) }
        return default
    }

    private fun normalize(config: CagConfig): CagConfig {
        if (config.slots.isEmpty()) return defaultConfig()
        config.slots.forEach { if (it.extension.isBlank()) it.extension = "java" }
        return config
    }

    private fun configPath(): Path? {
        val base = project.basePath ?: return null
        return Path.of(base, CagConfig.CONFIG_DIR, CagConfig.CONFIG_FILE)
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): CagConfigService = project.service()

        fun defaultConfig(): CagConfig = CagConfig(
            version = 1,
            architectureRoot = "",
            slots = mutableListOf(
                SlotConfig("model", "domain/model", "{Feature}", "java", "model"),
                SlotConfig("useCase", "domain/ports/usecase/{feature}", "{Feature}UseCase", "java", "usecase"),
                SlotConfig("useCaseImpl", "application/usecase/{feature}", "{Feature}UseCaseImpl", "java", "usecaseImpl"),
                SlotConfig("controller", "infrastructure/adapter/controller", "{Feature}Controller", "java", "controller"),
                SlotConfig("repository", "domain/ports/persistence", "{Feature}Repository", "java", "repository"),
                SlotConfig("repositoryImpl", "infrastructure/persistence", "{Feature}RepositoryImpl", "java", "repositoryImpl"),
                SlotConfig("entity", "infrastructure/persistence/entity", "{Feature}Entity", "java", "entity"),
                SlotConfig("dto", "infrastructure/adapter/dto", "{Feature}Dto", "java", "dto"),
                SlotConfig("mapper", "infrastructure/mapper", "{Feature}Mapper", "java", "mapper"),
            ),
        )
    }
}
