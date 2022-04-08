package com.github.spragucm.intellijpluginprojectviewfolding.settings

import com.github.spragucm.intellijpluginprojectviewfolding.ProjectViewFoldingBundle.message
import com.github.spragucm.intellijpluginprojectviewfolding.projectView.FoldingTreeStructureProvider
import com.intellij.ide.projectView.impl.AbstractProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.observable.properties.GraphPropertyImpl.Companion.graphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.layout.*
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.BorderFactory.createEmptyBorder

class ProjectViewFoldingConfigurable(private val project: Project) : SearchableConfigurable {

    companion object {
        const val ID = "com.github.spragucm.intellijpluginprojectviewfolding.options.ProjectViewFoldingConfigurable"
    }

    private val settings = project.service<ProjectSettings>()
    private val propertyGraph = PropertyGraph()
    private val foldingEnabledProperty = propertyGraph.graphProperty { settings.foldingEnabled }
    private val foldDirectoriesProperty = propertyGraph.graphProperty { settings.foldDirectories }
    private val foldIgnoredFilesProperty = propertyGraph.graphProperty { settings.foldIgnoredFiles }
    private val hideEmptyGroupsProperty = propertyGraph.graphProperty { settings.hideEmptyGroups }
    private val hideAllGroupsProperty = propertyGraph.graphProperty { settings.hideAllGroups }
    private val caseInsensitiveProperty = propertyGraph.graphProperty { settings.caseInsensitive }
    private val patternsProperty = propertyGraph.graphProperty { settings.patterns ?: "" }

    private lateinit var foldingEnabledPredicate: ComponentPredicate

    private val settingsPanel = panel {
        blockRow {
            row {
                checkBox(
                    message("projectViewFolding.settings.foldingEnabled"),
                    foldingEnabledProperty,
                )
                    .withSelectedBinding(settings::foldingEnabled.toBinding())
                    .comment(message("projectViewFolding.settings.foldingEnabled.comment"), -1)
                    .applyToComponent { setMnemonic('e') }
                    .apply { foldingEnabledPredicate = selected }
            }

            row {
                checkBox(
                    message("projectViewFolding.settings.foldDirectories"),
                    foldDirectoriesProperty,
                )
                    .withSelectedBinding(settings::foldDirectories.toBinding())
                    .comment(message("projectViewFolding.settings.foldDirectories.comment"), -1)
                    .applyToComponent { setMnemonic('d') }
                    .enableIf(foldingEnabledPredicate)
            }

            row {
                checkBox(
                    message("projectViewFolding.settings.foldIgnoredFiles"),
                    foldIgnoredFilesProperty,
                )
                    .withSelectedBinding(settings::foldIgnoredFiles.toBinding())
                    .comment(message("projectViewFolding.settings.foldIgnoredFiles.comment"), -1)
                    .applyToComponent { setMnemonic('h') }
                    .enableIf(foldingEnabledPredicate)
            }

            row {
                checkBox(
                    message("projectViewFolding.settings.hideEmptyGroups"),
                    hideEmptyGroupsProperty,
                )
                    .withSelectedBinding(settings::hideEmptyGroups.toBinding())
                    .comment(message("projectViewFolding.settings.hideEmptyGroups.comment"), -1)
                    .applyToComponent { setMnemonic('h') }
                    .enableIf(foldingEnabledPredicate)
                    .apply { hideAllGroupsProperty.afterPropagation { enabled = !hideAllGroupsProperty.get() } }
            }

            row {
                checkBox(
                    message("projectViewFolding.settings.hideAllGroups"),
                    hideAllGroupsProperty,
                )
                    .withSelectedBinding(settings::hideAllGroups.toBinding())
                    .comment(message("projectViewFolding.settings.hideAllGroups.comment"), -1)
                    .applyToComponent { setMnemonic('i') }
                    .enableIf(foldingEnabledPredicate)

                ContextHelpLabel.create(
                    message("projectViewFolding.settings.hideAllGroups.help"),
                    message("projectViewFolding.settings.hideAllGroups.help.description"),
                )()
            }

            row {
                checkBox(
                    message("projectViewFolding.settings.caseInsensitive"),
                    caseInsensitiveProperty,
                )
                    .withSelectedBinding(settings::caseInsensitive.toBinding())
                    .comment(message("projectViewFolding.settings.caseInsensitive.comment"), -1)
                    .applyToComponent { setMnemonic('c') }
                    .enableIf(foldingEnabledPredicate)
            }
        }

        titledRow(message("projectViewFolding.settings.foldingRules")) {
            row {
                expandableTextField(patternsProperty)
                    .withTextBinding(settings::patterns.toNullableBinding(""))
                    .comment(message("projectViewFolding.settings.patterns.comment"), -1)
                    .constraints(CCFlags.growX)
                    .applyToComponent {
                        emptyText.text = message("projectViewFolding.settings.patterns")
                    }
                    .enableIf(foldingEnabledPredicate)
            }
        }
    }

    private val projectView by lazy {
        object: ProjectViewPane(project) {
            override fun enableDnD() = Unit

            override fun createStructure() = object: AbstractProjectTreeStructure(project) {
                override fun getProviders() = listOf(FoldingTreeStructureProvider(project).apply {
                    propertyGraph.afterPropagation {
                        updateFromRoot(true)
                    }
                    withState(IProjectState.fromGraphProperties(
                        foldingEnabledProperty,
                        foldDirectoriesProperty,
                        hideEmptyGroupsProperty,
                        hideAllGroupsProperty,
                        foldIgnoredFilesProperty,
                        caseInsensitiveProperty,
                        patternsProperty
                    ))
                })
            }
        }
    }

    override fun getId() = ID

    override fun getDisplayName() = message("projectViewFolding.name")

    override fun createComponent() = OnePixelSplitter(.3f).apply {
        firstComponent = settingsPanel.apply {
            border = createEmptyBorder(10, 10, 10, 30)
        }
        secondComponent = projectView.createComponent().apply {
            border = createEmptyBorder()
        }
        TreeUtil.promiseExpand(projectView.tree, 2)
    }

    override fun isModified() = settingsPanel.isModified()

    override fun reset() = settingsPanel.reset()

    override fun apply() {
        val updated = isModified

        settingsPanel.apply()
        if (updated) {
            ApplicationManager.getApplication()
                .messageBus
                .syncPublisher(IProjectSettingsListener.TOPIC)
                .settingsChanged(settings)
        }
    }
}