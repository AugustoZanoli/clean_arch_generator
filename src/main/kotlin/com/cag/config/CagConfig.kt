package com.cag.config

data class ModuleConfig(
    var name: String = "",
    var slots: MutableList<SlotConfig> = mutableListOf(),
)

data class SlotConfig(
    var id: String = "",
    var subfolder: String = "",
    var fileName: String = "",
    var extension: String = "java",
    var template: String = "",
    var enabledByDefault: Boolean = true,
    @Transient var moduleName: String = "",
) {
    val folder: String get() = when {
        moduleName.isNotEmpty() && subfolder.isNotEmpty() -> "$moduleName/$subfolder"
        moduleName.isNotEmpty() -> moduleName
        else -> subfolder
    }
}

data class CagConfig(
    var version: Int = 1,
    var architectureRoot: String = "",
    var modules: MutableList<ModuleConfig> = mutableListOf(),
    @Transient var legacySlots: MutableList<SlotConfig> = mutableListOf(),
) {
    fun flattenSlots(): List<SlotConfig> = modules.flatMap { module ->
        module.slots.map { slot ->
            slot.copy(moduleName = module.name)
        }
    }

    fun hasLegacySlots(): Boolean = modules.isEmpty() && legacySlots.isNotEmpty()

    fun copyDeep(): CagConfig = CagConfig(
        version = version,
        architectureRoot = architectureRoot,
        modules = modules.map { module ->
            ModuleConfig(
                name = module.name,
                slots = module.slots.map { it.copy() }.toMutableList(),
            )
        }.toMutableList(),
    )

    companion object {
        const val CONFIG_DIR = ".cag"
        const val CONFIG_FILE = "config.json"
        const val TEMPLATES_DIR = "templates"
    }
}
