package com.cag.config

import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Serviço de projeto responsável por carregar, cachear e salvar o `.cag/config.json`.
 * Cria uma configuração default (a arquitetura de exemplo) na primeira vez que é lido,
 * se o arquivo ainda não existir.
 */
@Service(Service.Level.PROJECT)
class CagConfigService(private val project: Project) {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    @Volatile
    private var cached: CagConfig? = null

    /** Retorna a config, carregando do disco (ou default) na primeira chamada. */
    fun getConfig(): CagConfig {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = load()
            cached = loaded
            return loaded
        }
    }

    /** Força releitura do arquivo no próximo [getConfig]. */
    fun reload() {
        cached = null
    }

    /** Persiste a config no `.cag/config.json` e atualiza o cache. */
    fun save(config: CagConfig) {
        val path = configPath() ?: run {
            thisLogger().warn("Cag: projeto sem basePath; config não pôde ser salva.")
            return
        }
        Files.createDirectories(path.parent)
        Files.writeString(path, gson.toJson(config))
        cached = config.copyDeep()
    }

    private fun load(): CagConfig {
        val path = configPath()
        if (path != null && path.exists()) {
            runCatching { gson.fromJson(path.readText(), CagConfig::class.java) }
                .onSuccess { if (it != null) return normalize(it) }
                .onFailure { thisLogger().warn("Cag: falha ao ler ${path}, usando default.", it) }
        }
        // Arquivo ausente/ inválido: cria o default no disco e retorna.
        val default = defaultConfig()
        if (path != null) runCatching { save(default) }
        return default
    }

    /** Garante campos mínimos válidos após desserialização. */
    private fun normalize(config: CagConfig): CagConfig {
        if (config.slots.isEmpty()) return defaultConfig()
        config.slots.forEach { if (it.extension.isBlank()) it.extension = "java" }
        return config
    }

    private fun configPath(): Path? {
        val base = project.basePath ?: return null
        return Path.of(base, CagConfig.CONFIG_DIR, CagConfig.CONFIG_FILE)
    }

    companion object {
        fun getInstance(project: Project): CagConfigService = project.service()

        /** Configuração default embutida (a arquitetura clean de exemplo). */
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
