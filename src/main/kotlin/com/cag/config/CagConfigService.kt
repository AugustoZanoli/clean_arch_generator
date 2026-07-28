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
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.nameWithoutExtension

data class ConfigProfile(
    val name: String,
    val displayName: String,
)

@Service(Service.Level.PROJECT)
class CagConfigService(private val project: Project) : Disposable {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var lastModified: Long = 0

    @Volatile
    private var cached: CagConfig? = null

    private val profileCache = HashMap<String, CagConfig>()
    private val profileModified = HashMap<String, Long>()

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

    fun reload() { cached = null }

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

    fun discoverProfiles(): List<ConfigProfile> {
        val dir = configDir() ?: return emptyList()
        if (!dir.exists()) return emptyList()
        return runCatching {
            dir.listDirectoryEntries("*.json")
                .filter { it.fileName.toString() != CagConfig.CONFIG_FILE }
                .sortedBy { it.nameWithoutExtension.lowercase() }
                .map { ConfigProfile(it.nameWithoutExtension, it.nameWithoutExtension.replaceFirstChar { c -> c.uppercase() }) }
        }.getOrDefault(emptyList())
    }

    fun getProfile(name: String): CagConfig {
        val path = profilePath(name) ?: return defaultConfig()
        if (!path.exists()) return defaultConfig()
        val currentModified = path.toFile().lastModified()
        val cachedModified = profileModified[name]
        if (cachedModified != null && currentModified <= cachedModified) {
            profileCache[name]?.let { return it }
        }
        val loaded = loadFromPath(path)
        profileCache[name] = loaded
        profileModified[name] = currentModified
        return loaded
    }

    fun saveProfile(name: String, config: CagConfig) {
        val path = profilePath(name) ?: run {
            thisLogger().warn("Cag: projeto sem basePath; profile não pôde ser salvo.")
            return
        }
        Files.createDirectories(path.parent)
        Files.writeString(path, gson.toJson(config))
        profileCache[name] = config.copyDeep()
        profileModified[name] = path.toFile().lastModified()
    }

    private fun load(): CagConfig {
        val path = configPath()
        if (path != null && path.exists()) {
            val loaded = loadFromPath(path)
            if (loaded.modules.isNotEmpty()) return loaded
        }
        val default = defaultConfig()
        if (path != null) runCatching { save(default) }
        return default
    }

    private fun loadFromPath(path: Path): CagConfig {
        runCatching {
            val json = path.readText()
            val config = gson.fromJson(json, CagConfig::class.java)
            if (config != null) {
                val jsonObj = gson.fromJson(json, Map::class.java)
                if (jsonObj?.containsKey("slots") == true && !jsonObj.containsKey("modules")) {
                    @Suppress("UNCHECKED_CAST")
                    val legacySlots = jsonObj["slots"] as? List<Map<String, Any>>
                    if (legacySlots != null) {
                        config.legacySlots = legacySlots.map { slotMap ->
                            SlotConfig(
                                id = slotMap["id"] as? String ?: "",
                                subfolder = slotMap["folder"] as? String ?: "",
                                fileName = slotMap["fileName"] as? String ?: "",
                                extension = slotMap["extension"] as? String ?: "java",
                                template = slotMap["template"] as? String ?: "",
                                enabledByDefault = slotMap["enabledByDefault"] as? Boolean ?: true,
                            )
                        }.toMutableList()
                    }
                }
                return normalize(config)
            }
        }.onFailure { thisLogger().warn("Cag: falha ao ler $path, usando default.", it) }
        return defaultConfig()
    }

    private fun normalize(config: CagConfig): CagConfig {
        // Migrar slots legados para modules
        if (config.hasLegacySlots()) {
            migrateLegacySlots(config)
        }

        // Se ainda não tem modules, usar default
        if (config.modules.isEmpty()) return defaultConfig()

        // Normalizar extensions
        config.modules.forEach { module ->
            module.slots.forEach { slot ->
                if (slot.extension.isBlank()) slot.extension = "java"
            }
        }
        return config
    }

    private fun migrateLegacySlots(config: CagConfig) {
        val moduleMap = linkedMapOf<String, MutableList<SlotConfig>>()

        for (slot in config.legacySlots) {
            val folder = slot.subfolder // subfolder contains the old folder value
            val parts = folder.split("/", limit = 2)
            val moduleName = parts.firstOrNull() ?: "Default"
            val subfolder = parts.getOrNull(1) ?: ""

            moduleMap.getOrPut(moduleName) { mutableListOf() }.add(
                SlotConfig(
                    id = slot.id,
                    subfolder = subfolder,
                    fileName = slot.fileName,
                    extension = slot.extension,
                    template = slot.template,
                    enabledByDefault = slot.enabledByDefault,
                )
            )
        }

        config.modules = moduleMap.map { (name, slots) ->
            ModuleConfig(name, slots)
        }.toMutableList()
        config.legacySlots.clear()

        thisLogger().info("Cag: configuração legada migrada para estrutura modular")
    }

    private fun configDir(): Path? {
        val base = project.basePath ?: return null
        return Path.of(base, CagConfig.CONFIG_DIR)
    }

    private fun configPath(): Path? {
        return configDir()?.resolve(CagConfig.CONFIG_FILE)
    }

    private fun profilePath(name: String): Path? {
        return configDir()?.resolve("$name.json")
    }

    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): CagConfigService = project.service()

        fun defaultConfig(): CagConfig = CagConfig(
            version = 2,
            modules = mutableListOf(
                ModuleConfig("Domain", mutableListOf(
                    SlotConfig("model", "model", "{Feature}", "java", "model"),
                    SlotConfig("dto", "dto", "{Feature}Dto", "java", "dto"),
                    SlotConfig("useCase", "usecase", "{Feature}UseCase", "java", "usecase"),
                    SlotConfig("repository", "repository", "{Feature}Repository", "java", "repository"),
                )),
                ModuleConfig("Application", mutableListOf(
                    SlotConfig("useCaseImpl", "usecase", "{Feature}UseCaseImpl", "java", "usecaseImpl"),
                )),
                ModuleConfig("Infrastructure", mutableListOf(
                    SlotConfig("controller", "controller", "{Feature}Controller", "java", "controller"),
                    SlotConfig("repositoryImpl", "repository", "{Feature}RepositoryImpl", "java", "repositoryImpl"),
                    SlotConfig("entity", "entity", "{Feature}Entity", "java", "entity"),
                    SlotConfig("mapper", "mapper", "{Feature}Mapper", "java", "mapper"),
                )),
            ),
        )
    }
}
