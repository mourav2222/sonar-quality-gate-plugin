package de.cenit.phoenix.testinstallation.plugin

import groovy.json.JsonSlurper

class SonarQube {
    String authToken
    SonarReport sonarReport
    def jsonSlurper = new JsonSlurper()

    SonarQube(SonarReport sonarReport, authToken) {
        this.sonarReport = sonarReport
        this.authToken = "Basic ${(authToken + ":").getBytes().encodeBase64()}"
    }

    def checkQualityGate(Integer retryTimes, Integer intervalSeconds) {
        String analysisURL = sonarReport.getAnalysisUrl()
        println("analysisURL:" + analysisURL)
        for (int time in 1..retryTimes) {
            sleep(intervalSeconds * 1000)
            String analysisId = getAnalysisId(analysisURL)
            println("Get project status " + time + " times")
            String projectStatus = getProjectStatus(analysisId)
            boolean result = analysisQualityGateResult(projectStatus)
            if (result){
                break
            }
            if (time == retryTimes && projectStatus != "OK")
                throw new Exception("Check quality gate timeout in " + intervalSeconds * retryTimes + " seconds with "
                        + intervalSeconds + " intervalSeconds and tried " + retryTimes + " times")
        }
    }

    def setQualityGate(gateName) {
        if (gateName) {
            String projectStatusURL = sonarReport.getServerUrl() +
                    "/api/qualitygates/select?projectKey=${sonarReport.projectKey}&&gateName=${gateName}"
            println("selectQualityGateUrl:" + projectStatusURL)
            def selectQualityGateRequest = new URL(projectStatusURL).openConnection()
            selectQualityGateRequest.setRequestMethod("POST")
            selectQualityGateRequest.setRequestProperty("Authorization", authToken)
            def selectQualityGateResponseCode = selectQualityGateRequest.getResponseCode()
            if (selectQualityGateResponseCode == 204) {
                println("select QualityGate:${gateName} successful")
            } else {
                throw new Exception("select QualityGate:${gateName} failed, please confirm the selected gate name in ${getQualityGateNameList()}")
            }
        }
    }

    def getQualityGateNameList() {
        def qualityGateNameList = []
        String qualityGatesURL = sonarReport.getServerUrl() + "/api/qualitygates/list"
        println("qualityGatesURL:" + qualityGatesURL)
        def getQualityGatesRequest = new URL(qualityGatesURL).openConnection()
        getQualityGatesRequest.setRequestProperty("Authorization", authToken)
        def getQualityGatesResponseCode = getQualityGatesRequest.getResponseCode()
        println("getQualityGatesResponseCode:${getQualityGatesResponseCode}")
        if (getQualityGatesResponseCode == 200) {
            def getQualityGatesMessage = getQualityGatesRequest.getInputStream().getText()
            println("getQualityGatesMessage:${getQualityGatesMessage}")
            def getQualityGateMessageObject = jsonSlurper.parseText(getQualityGatesMessage)
            ArrayList qualityGateList = getQualityGateMessageObject.qualitygates
            qualityGateNameList = qualityGateList.collect { it.name }
        }
        qualityGateNameList
    }

    def getQualityGateName() {
        def qualityGateName = null
        String qualityGateURL = sonarReport.getServerUrl() +
                "/api/qualitygates/get_by_project?project=${sonarReport.projectKey}"
        println("qualityGateURL:" + qualityGateURL)
        def getQualityGateRequest = new URL(qualityGateURL).openConnection()
        getQualityGateRequest.setRequestProperty("Authorization", authToken)
        def getQualityGateResponseCode = getQualityGateRequest.getResponseCode()
        println("getQualityGateResponseCode:${getQualityGateResponseCode}")
        if (getQualityGateResponseCode == 200) {
            def getQualityGateMessage = getQualityGateRequest.getInputStream().getText()
            println("getQualityGateMessage:${getQualityGateMessage}")
            def getQualityGateMessageObject = jsonSlurper.parseText(getQualityGateMessage)
            qualityGateName = getQualityGateMessageObject.qualityGate.name
        }
        qualityGateName
    }

    def getAnalysisId(String analysisURL) {
        def analysisId = null
        def getAnalysisStatusRequest = new URL(analysisURL).openConnection()
        getAnalysisStatusRequest.setRequestProperty("Authorization", authToken)
        def analysisStatusCode = getAnalysisStatusRequest.getResponseCode()
        println("analysisStatusCode:${analysisStatusCode}")
        if (analysisStatusCode == 200) {
            def analysisStatusMessage = getAnalysisStatusRequest.getInputStream().getText()
            println("analysisStatusMessage:${analysisStatusMessage}")
            def analysisStatusObject = jsonSlurper.parseText(analysisStatusMessage)
            analysisId = analysisStatusObject.task.analysisId
        } else {
            println("SonarQube analysis task(Id:${sonarReport.getTaskId()}) still in the queue. ")
        }
        analysisId
    }

    def analysisQualityGateResult(projectStatus) {
        switch (projectStatus) {
            case null:
                break
            case "OK":
                println("******** Quality gate:${getQualityGateName()} passed **********")
                return true
            default:
                throw new Exception("Quality gate:${getQualityGateName()} failed, because does not meet the requirement")
        }
    }

    def getProjectStatus(analysisId) {
        def projectStatus = null
        if (analysisId) {
            String projectStatusURL = sonarReport.getServerUrl() +
                    "/api/qualitygates/project_status?analysisId=" + analysisId
            println("projectStatusURL:" + projectStatusURL)
            def projectStatusRequest = new URL(projectStatusURL).openConnection()
            projectStatusRequest.setRequestProperty("Authorization", authToken)
            def projectStatusCode = projectStatusRequest.getResponseCode()
            println("projectStatusCode:${projectStatusCode}")
            if (projectStatusCode == 200) {
                def projectStatusMessage = projectStatusRequest.getInputStream().getText()
                println("projectStatusMessage:${projectStatusMessage}")
                def projectStatusResultObject = jsonSlurper.parseText(projectStatusMessage)
                projectStatus = projectStatusResultObject.projectStatus.status
            }
        }
        projectStatus
    }
}
