package com.cag.core

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class FeatureGenerator(private val project: Project) {

    fun generate(plan: FeaturePlan): List<VirtualFile> {
        val created = mutableListOf<VirtualFile>()

        WriteCommandAction.runWriteCommandAction(project, "Gerar Feature ${plan.featureName}", null, {
            for (file in plan.files) {
                if (!file.enabled || file.alreadyExists) continue

                var dir = file.existingBaseDir
                for (segment in file.segmentsToCreate) {
                    dir = dir.findChild(segment)?.takeIf { it.isDirectory }
                        ?: dir.createChildDirectory(this, segment)
                }

                val virtualFile = dir.findChild(file.fileName) ?: dir.createChildData(this, file.fileName)
                VfsUtil.saveText(virtualFile, file.content)
                created += virtualFile
            }
        })

        if (created.isNotEmpty()) {
            val editorManager = FileEditorManager.getInstance(project)
            created.forEachIndexed { index, file -> editorManager.openFile(file, index == created.lastIndex) }
        }

        return created
    }
}
