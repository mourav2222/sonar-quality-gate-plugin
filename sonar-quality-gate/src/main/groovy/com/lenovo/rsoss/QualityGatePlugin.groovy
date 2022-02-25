package com.lenovo.rsoss

import org.gradle.api.Project
import org.gradle.api.Plugin

class QualityGatePlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create("qualityGate",QualityGateExtension)
        project.tasks.register("qualityGate",QualityGateTask)
    }
}
