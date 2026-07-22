package com.cag.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CagConfigInitializer : ProjectActivity {
    override suspend fun execute(project: Project) {
        CagConfigService.getInstance(project).getConfig()
    }
}
