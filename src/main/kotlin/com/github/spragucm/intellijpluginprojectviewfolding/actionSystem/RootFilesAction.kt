package com.github.spragucm.intellijpluginprojectviewfolding.actionSystem

import com.github.spragucm.intellijpluginprojectviewfolding.settings.ProjectSettings
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ToggleOptionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import java.util.function.Function

class RootFilesAction: DumbAware, ToggleOptionAction(Function {
    object : Option {
        private val settings = it.project?.service<ProjectSettings>()

        override fun isSelected() = settings?.foldingEnabled ?: false

        override fun setSelected(selected: Boolean) {
            val updated = selected != isSelected

            if (updated) {
                it.project?.let { project ->
                    val view = ProjectView.getInstance(project)
                    view.currentProjectViewPane?.updateFromRoot(true)
                }
            }
        }
    }
})