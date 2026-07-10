package com.cag.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Ação que abre a janela flutuante de geração de feature.
 * Registrada com atalho e ícone no plugin.xml.
 */
class GenerateFeatureAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val context = e.getData(CommonDataKeys.VIRTUAL_FILE)
        GenerateFeatureDialog(project, context).show()
    }
}
