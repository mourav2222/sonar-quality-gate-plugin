package de.cenit.phoenix.testinstallation.plugin

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static com.github.tomakehurst.wiremock.client.WireMock.*

class QualityPluginFunctionalTest extends Specification {
    public static final String TASK_NAME = "qualityGate"
    public static final String ANALYSIS_ID = "test_analysisId"
    public static final String PROJECT_KEY = "test_project_key"
    public static final String GATE_NAME = "test_gate_name"
    @TempDir
    private File projectDir

    private getBuildFile() {
        new File(projectDir, "build.gradle")
    }

    private getSettingsFile() {
        new File(projectDir, "settings.gradle")
    }

    private getGradlePropertiesFile() {
        new File(projectDir, "gradle.properties")
    }

    private getReportTaskFile() {
        def reportDir = projectDir.getPath() + "/build/sonar"
        new File(reportDir).mkdirs()
        new File(reportDir, "report-task.txt")
    }

    WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort())

    def setup() {
        wireMockServer.start()
        settingsFile << ""
        buildFile << """
plugins {
    id('de.cenit.phoenix.testinstallation.plugin.sonar-quality-gate')
}

qualityGate {
    gateName = '${GATE_NAME}'
    retryTimes = 1
    intervalSeconds = 1
}
"""
        reportTaskFile << """
projectKey=${PROJECT_KEY}
serverUrl=http://localhost:${wireMockServer.port()}
ceTaskId=abc
ceTaskUrl=http://localhost:${wireMockServer.port()}/api/ce/task?id=abc
"""

        gradlePropertiesFile << """
sonar.login = test
"""

        wireMockServer.stubFor(post(urlEqualTo("/api/qualitygates/select?projectKey=${PROJECT_KEY}&&gateName=${GATE_NAME}"))
                .willReturn(aResponse().withStatus(204)))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/get_by_project?project=${PROJECT_KEY}"))
                .willReturn(aResponse().withStatus(200).withBody("{\"qualityGate\": {\"name\": \"${GATE_NAME}\"}}")))
    }

    def cleanup() {
        wireMockServer.stop()
    }

    def "Analysis task in the queue should throw Exception"() {
        given:
        wireMockServer.stubFor(get(urlEqualTo("/api/ce/task?id=abc"))
                .willReturn(aResponse().withStatus(400)))
        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        runner.build()

        then:
        def error = thrown(Exception)
        error.message.contains("SonarQube analysis task(Id:abc) still in the queue.")
    }


    def "Analysis timeout should throw Exception"() {
        given:
        buildFile.text = """
plugins {
    id('de.cenit.phoenix.testinstallation.plugin.sonar-quality-gate')
}

qualityGate {
    gateName = '${GATE_NAME}'
    retryTimes = 2
    intervalSeconds = 1
}
"""

        wireMockServer.stubFor(get(urlEqualTo("/api/ce/task?id=abc"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"task\": {\"analysisId\": \"${ANALYSIS_ID}\"}}")))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/project_status?analysisId=${ANALYSIS_ID}"))
                .willReturn(aResponse().withStatus(400)))

        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        runner.build()

        then:
        def error = thrown(Exception)
        error.message.contains("Check quality gate timeout in 2 seconds with 1 intervalSeconds and tried 2 times")
    }


    def "Quality gate failed should throw Exception"() {
        given:
        wireMockServer.stubFor(get(urlEqualTo("/api/ce/task?id=abc"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"task\": {\"analysisId\": \"${ANALYSIS_ID}\"}}")))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/project_status?analysisId=${ANALYSIS_ID}"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"projectStatus\": {\"status\": \"FAILED\"}}")))

        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        runner.build()

        then:
        def error = thrown(Exception)
        error.message.contains("Quality gate:${GATE_NAME} failed, because does not meet the requirement")
    }

    def "Quality gate can pass with specified gate"() {
        given:
        wireMockServer.stubFor(get(urlEqualTo("/api/ce/task?id=abc"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"task\": {\"analysisId\": \"${ANALYSIS_ID}\"}}")))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/project_status?analysisId=${ANALYSIS_ID}"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"projectStatus\": {\"status\": \"OK\"}}")))

        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        def result = runner.build()

        then:
        result.output.contains("select QualityGate:${GATE_NAME} successful")
        result.output.contains("******** Quality gate:${GATE_NAME} passed **********")
    }

    def "select No-exist gate name should throw Exception"() {
        given:
        wireMockServer.stubFor(post(urlEqualTo("/api/qualitygates/select?projectKey=${PROJECT_KEY}&&gateName=${GATE_NAME}"))
                .willReturn(aResponse().withStatus(404)))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/list"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"qualitygates\": [{\"name\": \"Grade1\"}, {\"name\": \"Grade2\"}, {\"name\": \"Sonar way\"}]}")))

        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        runner.build()

        then:
        def error = thrown(Exception)
        error.message.contains("select QualityGate:${GATE_NAME} failed, please confirm the selected gate name in [Grade1, Grade2, Sonar way]")
    }

    def "Unselect quality gate can pass with current gate"() {
        given:
        buildFile.text = """
plugins {
    id('de.cenit.phoenix.testinstallation.plugin.sonar-quality-gate')
}

qualityGate {
    retryTimes = 1
    intervalSeconds = 1
}
"""
        wireMockServer.stubFor(get(urlEqualTo("/api/ce/task?id=abc"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"task\": {\"analysisId\": \"${ANALYSIS_ID}\"}}")))
        wireMockServer.stubFor(get(urlEqualTo("/api/qualitygates/project_status?analysisId=${ANALYSIS_ID}"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"projectStatus\": {\"status\": \"OK\"}}")))

        when:
        def runner = GradleRunner.create()
        configRunner(runner)
        def result = runner.build()

        then:
        !result.output.contains("select QualityGate")
        result.output.contains("******** Quality gate:${GATE_NAME} passed **********")
    }

    private void configRunner(GradleRunner runner) {
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(TASK_NAME)
        runner.withProjectDir(projectDir)
    }
}
