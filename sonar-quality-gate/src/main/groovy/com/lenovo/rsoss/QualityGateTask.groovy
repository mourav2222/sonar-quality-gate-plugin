package com.lenovo.rsoss

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class QualityGateTask extends DefaultTask {
    QualityGateTask() {
        setGroup("verification")
        setDescription("Verify that the quality indicators meet the quality gates specified on SonarQube")
    }

    @TaskAction
    def action() {
        def authToken = this.project.properties.get("systemProp.sonar.login")
        def gateName = project.qualityGate.getGateName().getOrElse(null)
        def retryTimes = project.qualityGate.getRetryTimes().get()
        def intervalSeconds = project.qualityGate.getIntervalSeconds().get()
        println("authToken:${authToken}")
        println("gateName:${gateName}")
        println("retryTimes:${retryTimes}")
        println("intervalSeconds:${intervalSeconds}")
        SonarReport sonarReport = new SonarReport(this.getProject().getBuildDir())
        SonarQube sonarQube = new SonarQube(sonarReport, authToken)
        sonarQube.setQualityGate(gateName)
        sonarQube.checkQualityGate(retryTimes, intervalSeconds)
    }
}
