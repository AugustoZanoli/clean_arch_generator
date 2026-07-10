package com.cag.config

/**
 * Um "slot" da arquitetura: um tipo de arquivo que é gerado para cada feature
 * (ex.: UseCase, Dto, Controller...).
 *
 * - [id]        identificador único, usado nas variáveis de template `{fqn.<id>}` etc.
 * - [folder]    caminho da pasta relativo à raiz da arquitetura (ex.: "domain/models").
 * - [fileName]  padrão do nome do arquivo, sem extensão (ex.: "{Feature}Dto").
 * - [extension] extensão do arquivo (ex.: "java", "kt").
 * - [template]  nome do template (procurado em `.cag/templates/<template>.template`,
 *               com fallback para os defaults embutidos no plugin).
 * - [enabledByDefault] se o arquivo já vem marcado no preview.
 */
data class SlotConfig(
    var id: String = "",
    var folder: String = "",
    var fileName: String = "",
    var extension: String = "java",
    var template: String = "",
    var enabledByDefault: Boolean = true,
)

/**
 * Configuração completa da arquitetura de um projeto, persistida em `.cag/config.json`.
 *
 * [architectureRoot] é opcional: quando vazio, o plugin auto-detecta o source root
 * (e, se houver, o subdiretório de base package que contém as pastas da arquitetura).
 */
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
