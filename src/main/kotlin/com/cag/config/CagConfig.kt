package com.cag.config

data class SlotConfig(
    var id: String = "",
    var folder: String = "",
    var fileName: String = "",
    var extension: String = "java",
    var template: String = "",
    var enabledByDefault: Boolean = true,
)

data class CagConfig(
    var version: Int = 1,
    var architectureRoot: String = "",
    var slots: MutableList<SlotConfig> = mutableListOf(),
) {
    fun copyDeep(): CagConfig = CagConfig(
        version = version,
        architectureRoot = architectureRoot,
        slots = slots.map { it.copy() }.toMutableList(),
    )

    companion object {
        const val CONFIG_DIR = ".cag"
        const val CONFIG_FILE = "config.json"
        const val TEMPLATES_DIR = "templates"
    }
}
