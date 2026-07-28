package com.cag.ui

import com.cag.config.CagConfig
import com.cag.config.CagConfigService
import com.cag.config.ConfigProfile
import com.cag.config.ModuleConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel

class CagConfigurable(private val project: Project) : Configurable {

    private val profileField = ComboBox<ConfigProfile>()
    private val newProfileField = JBTextField(12)
    private val addProfileButton = JButton("Adicionar")
    private val architectureRootField = JBTextField()

    // Módulos
    private val moduleTableModel = ListTableModel<ModuleConfig>(
        ModuleColumnName(),
    )
    private val moduleTable = TableView<ModuleConfig>(moduleTableModel).apply {
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        setShowGrid(false)
    }

    // Slots do módulo selecionado
    private val slotTableModel = ListTableModel<SlotConfig>(
        StringColumn("id") { it::id },
        StringColumn("Subpasta") { it::subfolder },
        StringColumn("Nome do arquivo") { it::fileName },
        StringColumn("Extensão") { it::extension },
        StringColumn("Template") { it::template },
        BooleanColumn("Marcado por padrão") { it::enabledByDefault },
    )
    private val slotTable = TableView<SlotConfig>(slotTableModel)

    private var currentModules = mutableListOf<ModuleConfig>()

    private var currentProfileName: String? = null // null = Default (config.json)

    init {
        profileField.renderer = object : SimpleListCellRenderer<ConfigProfile>() {
            override fun customize(
                list: javax.swing.JList<out ConfigProfile>,
                value: ConfigProfile?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                text = value?.displayName ?: "Default"
            }
        }
        profileField.addActionListener { onProfileChanged() }
        addProfileButton.addActionListener { onAddProfile() }
        moduleTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                onModuleSelectionChanged()
            }
        }
    }

    override fun getDisplayName(): String = "Cag — Clean Architecture Generator"

    override fun createComponent(): JComponent {
        val profileRow = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Perfil:"), BorderLayout.WEST)
            add(profileField, BorderLayout.CENTER)
            val newProfilePanel = JPanel(BorderLayout(JBUI.scale(4), 0)).apply {
                add(newProfileField, BorderLayout.CENTER)
                add(addProfileButton, BorderLayout.EAST)
            }
            add(newProfilePanel, BorderLayout.EAST)
        }

        val archRow = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Raiz da arquitetura (relativa ao source root; vazio = auto-detectar):"), BorderLayout.WEST)
            add(architectureRootField, BorderLayout.CENTER)
        }

        val north = JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(profileRow, BorderLayout.NORTH)
            add(archRow, BorderLayout.SOUTH)
        }

        // Painel de módulos (esquerda)
        val modulePanel = JPanel(BorderLayout()).apply {
            val decorator = ToolbarDecorator.createDecorator(moduleTable)
                .setAddAction {
                    val newModule = ModuleConfig(name = "NovoModulo")
                    currentModules.add(newModule)
                    moduleTableModel.items = currentModules.toList()
                    moduleTable.selectionModel.clearSelection()
                    moduleTable.selectionModel.setSelectionInterval(
                        currentModules.size - 1,
                        currentModules.size - 1,
                    )
                }
                .setRemoveAction {
                    val row = moduleTable.selectedRow
                    if (row >= 0) {
                        val modelRow = moduleTable.convertRowIndexToModel(row)
                        currentModules.removeAt(modelRow)
                        moduleTableModel.items = currentModules.toList()
                        slotTableModel.items = emptyList()
                    }
                }
                .createPanel()
            add(decorator, BorderLayout.CENTER)
        }

        // Painel de slots (direita)
        val slotPanel = JPanel(BorderLayout()).apply {
            val decorator = ToolbarDecorator.createDecorator(slotTable)
                .setAddAction {
                    val selectedRow = moduleTable.selectedRow
                    if (selectedRow >= 0) {
                        val modelRow = moduleTable.convertRowIndexToModel(selectedRow)
                        val module = currentModules[modelRow]
                        val newSlot = SlotConfig(id = "novoSlot")
                        module.slots.add(newSlot)
                        slotTableModel.items = module.slots.toList()
                    }
                }
                .setRemoveAction {
                    val moduleRow = moduleTable.selectedRow
                    val slotRow = slotTable.selectedRow
                    if (moduleRow >= 0 && slotRow >= 0) {
                        val modelRow = moduleTable.convertRowIndexToModel(moduleRow)
                        val slotModelRow = slotTable.convertRowIndexToModel(slotRow)
                        val module = currentModules[modelRow]
                        module.slots.removeAt(slotModelRow)
                        slotTableModel.items = module.slots.toList()
                    }
                }
                .createPanel()
            add(decorator, BorderLayout.CENTER)
        }

        val splitPane = JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            modulePanel,
            slotPanel,
        ).apply {
            dividerLocation = 200
            resizeWeight = 0.3
        }

        val content = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(10)
            add(north, BorderLayout.NORTH)
            add(splitPane, BorderLayout.CENTER)
        }

        reset()
        return content
    }

    private fun onModuleSelectionChanged() {
        val selectedRow = moduleTable.selectedRow
        if (selectedRow >= 0) {
            val modelRow = moduleTable.convertRowIndexToModel(selectedRow)
            val module = currentModules[modelRow]
            slotTableModel.items = module.slots.toList()
        } else {
            slotTableModel.items = emptyList()
        }
    }

    override fun isModified(): Boolean {
        val service = CagConfigService.getInstance(project)
        val currentConfig = if (currentProfileName != null) {
            service.getProfile(currentProfileName!!)
        } else {
            service.getConfig()
        }
        return uiToConfig() != currentConfig
    }

    override fun apply() {
        val service = CagConfigService.getInstance(project)
        val config = uiToConfig()
        if (currentProfileName != null) {
            service.saveProfile(currentProfileName!!, config)
        } else {
            service.save(config)
        }
        service.reload()
    }

    override fun reset() {
        populateProfileComboBox()
        val service = CagConfigService.getInstance(project)
        val config = if (currentProfileName != null) {
            service.getProfile(currentProfileName!!).copyDeep()
        } else {
            service.getConfig().copyDeep()
        }
        loadConfigIntoUI(config)
    }

    private fun populateProfileComboBox() {
        val service = CagConfigService.getInstance(project)
        val profiles = service.discoverProfiles()
        profileField.removeAllItems()
        profiles.forEach { profileField.addItem(it) }
    }

    private fun onProfileChanged() {
        val selected = profileField.selectedItem as? ConfigProfile
        currentProfileName = selected?.name
        val service = CagConfigService.getInstance(project)
        val config = if (currentProfileName != null) {
            service.getProfile(currentProfileName!!).copyDeep()
        } else {
            service.getConfig().copyDeep()
        }
        loadConfigIntoUI(config)
    }

    private fun onAddProfile() {
        val name = newProfileField.text.trim().lowercase()
        if (name.isEmpty()) return
        val service = CagConfigService.getInstance(project)
        val profiles = service.discoverProfiles()
        if (profiles.any { it.name == name }) return
        service.saveProfile(name, CagConfigService.defaultConfig())
        populateProfileComboBox()
        val newProfile = ConfigProfile(name, name.replaceFirstChar { it.uppercase() })
        profileField.selectedItem = newProfile
        newProfileField.text = ""
    }

    private fun loadConfigIntoUI(config: CagConfig) {
        architectureRootField.text = config.architectureRoot
        currentModules = config.modules
        moduleTableModel.items = currentModules.toList()
        slotTableModel.items = emptyList()
    }

    private fun uiToConfig(): CagConfig = CagConfig(
        version = 2,
        architectureRoot = architectureRootField.text.trim(),
        modules = currentModules.map { module ->
            ModuleConfig(
                name = module.name,
                slots = module.slots.map { it.copy() }.toMutableList(),
            )
        }.toMutableList(),
    )

    private class ModuleColumnName : ColumnInfo<ModuleConfig, String>("Módulo") {
        override fun valueOf(item: ModuleConfig): String = item.name
        override fun isCellEditable(item: ModuleConfig): Boolean = true
        override fun setValue(item: ModuleConfig, value: String) {
            item.name = value
        }
    }

    private class StringColumn(
        name: String,
        private val prop: (SlotConfig) -> kotlin.reflect.KMutableProperty0<String>,
    ) : ColumnInfo<SlotConfig, String>(name) {
        override fun valueOf(item: SlotConfig): String = prop(item).get()
        override fun isCellEditable(item: SlotConfig): Boolean = true
        override fun setValue(item: SlotConfig, value: String) { prop(item).set(value) }
    }

    private class BooleanColumn(
        name: String,
        private val prop: (SlotConfig) -> kotlin.reflect.KMutableProperty0<Boolean>,
    ) : ColumnInfo<SlotConfig, Boolean>(name) {
        override fun valueOf(item: SlotConfig): Boolean = prop(item).get()
        override fun isCellEditable(item: SlotConfig): Boolean = true
        override fun getColumnClass(): Class<*> = Boolean::class.javaObjectType
        override fun setValue(item: SlotConfig, value: Boolean) { prop(item).set(value) }
    }
}
