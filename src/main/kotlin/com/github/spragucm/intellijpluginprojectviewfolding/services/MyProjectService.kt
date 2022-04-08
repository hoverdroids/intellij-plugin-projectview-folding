package com.github.spragucm.intellijpluginprojectviewfolding.services

import com.intellij.openapi.project.Project
import com.github.spragucm.intellijpluginprojectviewfolding.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
