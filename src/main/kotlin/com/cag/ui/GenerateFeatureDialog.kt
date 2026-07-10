package com.cag.ui

import com.cag.config.CagConfigService
import com.cag.core.ArchitectureResolver
import com.cag.core.FeaturePlan
import com.cag.core.FeatureGenerator
import com.cag.core.PlanBuilder
import com.cag.core.PlannedFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckboxTreeBase
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.tree.DefaultTreeModel

/**
 * Janela flutuante de geração: campo com o nome da feature + árvore de preview com checkboxes.
 * O preview é reconstruído a cada tecla; ao confirmar, gera os arquivos marcados.
 */
class GenerateFeatureDialog(
    private val project: Project,
    private val context: VirtualFile?,
) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val statusLabel = JBLabel(" ")
    private val rootNode = CheckedTreeNode("root")
    private val tree = CheckboxTree(
        PreviewRenderer(),
        rootNode,
        // Marcar/desmarcar uma pasta propaga para os arquivos; alterar um arquivo não mexe na pasta.
        CheckboxTreeBase.CheckPolicy(true, true, false, false),
    )
    private val treeModel = tree.model as DefaultTreeModel

    private var currentPlan: FeaturePlan? = null

    init {
        title = "Gerar Feature (Clean Architecture)"
        setOKButtonText("Criar arquivos")
        nameField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refreshPreview()
        })
        init()
        refreshPreview()
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun createCenterPanel(): JComponent {
        val north = JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
            add(JBLabel("Nome da feature:"), BorderLayout.WEST)
            add(nameField, BorderLayout.CENTER)
        }

        val scroll = com.intellij.ui.ScrollPaneFactory.createScrollPane(tree).apply {
            preferredSize = Dimension(JBUI.scale(520), JBUI.scale(320))
        }

        statusLabel.foreground = JBUI.CurrentTheme.Label.disabledForeground()

        return JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(10)
            add(north, BorderLayout.NORTH)
            add(scroll, BorderLayout.CENTER)
            add(statusLabel, BorderLayout.SOUTH)
        }
    }

    private fun refreshPreview() {
        val plan = computePlan()
        currentPlan = plan
        rebuildTree(plan)
        statusLabel.text = when {
            nameField.text.isBlank() -> "Digite o nome da feature para ver o preview."
            plan == null -> "Nenhum source root encontrado neste projeto."
            else -> {
                val toCreate = plan.files.count { it.enabled && !it.alreadyExists }
                val existing = plan.files.count { it.alreadyExists }
                "Serão criados $toCreate arquivo(s)" + if (existing > 0) " · $existing já existe(m)." else "."
            }
        }
    }

    private fun computePlan(): FeaturePlan? {
        val raw = nameField.text
        if (raw.isBlank()) return null
        val config = CagConfigService.getInstance(project).getConfig()
        val arch = ArchitectureResolver(project).resolve(config, context) ?: return null
        return PlanBuilder(project).build(raw, config, arch)
    }

    private fun rebuildTree(plan: FeaturePlan?) {
        rootNode.removeAllChildren()
        if (plan != null) {
            val dirNodes = HashMap<String, CheckedTreeNode>()
            for (file in plan.files) {
                val segments = file.relativePath.split('/').filter { it.isNotEmpty() }
                val dirSegments = segments.dropLast(1)

                var parent: CheckedTreeNode = rootNode
                var cumulative = ""
                for (seg in dirSegments) {
                    cumulative = if (cumulative.isEmpty()) seg else "$cumulative/$seg"
                    val currentParent = parent
                    parent = dirNodes.getOrPut(cumulative) {
                        CheckedTreeNode(seg).also { currentParent.add(it) }
                    }
                }
                parent.add(FileNode(file).apply {
                    isChecked = file.enabled
                    isEnabled = !file.alreadyExists
                })
            }
        }
        treeModel.reload()
        TreeUtil.expandAll(tree)
    }

    override fun doValidate(): ValidationInfo? {
        if (nameField.text.isBlank()) return ValidationInfo("Digite o nome da feature.", nameField)
        val plan = currentPlan ?: return ValidationInfo("Nenhum source root encontrado neste projeto.", nameField)
        if (plan.files.none { it.enabled && !it.alreadyExists }) {
            return ValidationInfo("Nenhum arquivo selecionado para criar.", nameField)
        }
        return null
    }

    override fun doOKAction() {
        val plan = currentPlan ?: return
        applyCheckboxSelection(plan)
        FeatureGenerator(project).generate(plan)
        super.doOKAction()
    }

    /** Sincroniza o estado dos checkboxes de volta para os [PlannedFile] antes de gerar. */
    private fun applyCheckboxSelection(plan: FeaturePlan) {
        val checkedFiles = HashSet<PlannedFile>()
        val nodes = rootNode.depthFirstEnumeration()
        while (nodes.hasMoreElements()) {
            val node = nodes.nextElement()
            if (node is FileNode && node.isChecked && node.isEnabled) checkedFiles += node.plannedFile
        }
        plan.files.forEach { it.enabled = it in checkedFiles }
    }

    /** Nó de arquivo na árvore de preview. */
    private class FileNode(val plannedFile: PlannedFile) : CheckedTreeNode(plannedFile)

    /** Renderer que mostra pastas/arquivos e marca os já existentes. */
    private class PreviewRenderer : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: javax.swing.JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            when (val node = value) {
                is FileNode -> {
                    val file = node.plannedFile
                    textRenderer.icon = AllIcons.FileTypes.Any_type
                    textRenderer.append(file.fileName)
                    if (file.alreadyExists) {
                        textRenderer.append("  (já existe — será pulado)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                    }
                }
                is CheckedTreeNode -> {
                    textRenderer.icon = AllIcons.Nodes.Folder
                    textRenderer.append(node.userObject?.toString() ?: "", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
                }
            }
        }
    }
}
