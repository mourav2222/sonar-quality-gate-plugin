package com.lenovo.rsoss

class SonarReport {
    def taskProperties

    SonarReport(buildDir) {
        def reportFile = new File(buildDir, "sonar/report-task.txt")
        def props = new Properties()
        props.load(new FileInputStream(reportFile))
        this.taskProperties = props
    }

    def getTaskId() {
        return taskProperties.getProperty("ceTaskId")
    }

    def getAnalysisUrl() {
        return taskProperties.getProperty("ceTaskUrl")
    }

    def getServerUrl(){
        return taskProperties.getProperty("serverUrl")
    }

    def getProjectKey(){
        return taskProperties.getProperty("projectKey")
    }
}
