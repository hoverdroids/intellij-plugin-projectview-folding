package com.github.spragucm.intellijpluginprojectviewfolding.settings

import com.intellij.util.messages.Topic
import java.util.EventListener

@FunctionalInterface
interface IProjectSettingsListener: EventListener {

    companion object {
        @Topic.ProjectLevel
        val TOPIC = Topic(IProjectSettingsListener::class.java)
    }

    fun settingsChanged(settings: ProjectSettings)
}