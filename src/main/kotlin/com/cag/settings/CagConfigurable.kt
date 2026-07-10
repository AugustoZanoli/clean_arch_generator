package com.cag.settings

import com.cag.config.CagConfig
import com.cag.config.CagConfigService
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

/**
 * Tela em Settings → Tools → Cag para editar visualmente o `.cag/config.json`.
 * Fonte da verdade continua sendo o arquivo; esta tela apenas o lê e reescreve.
 */
class CagConfigurable(private val project: Project) : Configurable {

    private val architectureRootField = JBTextField()
    private val tableModel = ListTableModel<SlotConfig>(
        StringColumn("id") { it::id },
        StringColumn("Pasta") { it::folder },
        StringColumn("Nome do arquivo") { it::fileName },
        StringColumn("Extensão") { it::extension },
        StringColumn("Template") { it::template },
        BooleanColumn("Marcado por padrão") { it::enabledByDefault },
    )
    private val table = TableView<SlotConfig>(tableModel)

    override fun getDisplayName(): String = "Cag — Clean Architecture Generator"

    override fun createComponent(): JComponent {
        val north = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Raiz da arquitetura (relativa ao source root; vazio = auto-detectar):"), BorderLayout.WEST)
            add(architectureRootField, BorderLayout.CENTER)
        }

        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction { tableModel.addRow(SlotConfig(id = "novoSlot")) }
            .setRemoveAction {
                val row = table.selectedRow
                if (row >= 0) tableModel.removeRow(table.convertRowIndexToModel(row))
            }
            .createPanel()

        reset()
        return JPanel(BorderLayout(0, JBUI.scale(10))).apply {
            border = JBUI.Borders.empty(10)
            add(north, BorderLayout.NORTH)
            add(tablePanel, BorderLayout.CENTER)
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
        tableModel.items = config.slots
    }

    private fun uiToConfig(): CagConfig = CagConfig(
        version = 1,
        architectureRoot = architectureRootField.text.trim(),
        slots = tableModel.items.map { it.copy() }.toMutableList(),
    )

    /** Coluna editável ligada a uma propriedade String mutável do [SlotConfig]. */
    private class StringColumn(
        name: String,
        private val prop: (SlotConfig) -> kotlin.reflect.KMutableProperty0<String>,
    ) : ColumnInfo<SlotConfig, String>(name) {
        override fun valueOf(item: SlotConfig): String = prop(item).get()
        override fun isCellEditable(item: SlotConfig): Boolean = true
        override fun setValue(item: SlotConfig, value: String) {
            prop(item).set(value)
        }
    }

    /** Coluna editável (checkbox) ligada a uma propriedade Boolean mutável do [SlotConfig]. */
    private class BooleanColumn(
        name: String,
        private val prop: (SlotConfig) -> kotlin.reflect.KMutableProperty0<Boolean>,
    ) : ColumnInfo<SlotConfig, Boolean>(name) {
        override fun valueOf(item: SlotConfig): Boolean = prop(item).get()
        override fun isCellEditable(item: SlotConfig): Boolean = true
        override fun getColumnClass(): Class<*> = Boolean::class.javaObjectType
        override fun setValue(item: SlotConfig, value: Boolean) {
            prop(item).set(value)
        }
    }
}
