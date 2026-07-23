package com.cag.ui

import com.cag.config.CagConfig
import com.cag.config.CagConfigService
import com.cag.config.ModuleConfig
import com.cag.config.SlotConfig
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel

class CagConfigurable(private val project: Project) : Configurable {

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

    init {
        moduleTable.selectionModel.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                onModuleSelectionChanged()
            }
        }
    }

    override fun getDisplayName(): String = "Cag — Clean Architecture Generator"

    override fun createComponent(): JComponent {
        val north = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Raiz da arquitetura (relativa ao source root; vazio = auto-detectar):"), BorderLayout.WEST)
            add(architectureRootField, BorderLayout.CENTER)
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

    override fun isModified(): Boolean = uiToConfig() != CagConfigService.getInstance(project).getConfig()

    override fun apply() {
        val service = CagConfigService.getInstance(project)
        service.save(uiToConfig())
        service.reload()
    }

    override fun reset() {
        val config = CagConfigService.getInstance(project).getConfig().copyDeep()
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
