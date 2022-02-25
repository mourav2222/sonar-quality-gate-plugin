package com.lenovo.rsoss

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class QualityGatePluginTest extends Specification {
    def "plugin has qualityGate task"() {
        given:
        def project = ProjectBuilder.builder().build()

        when:
        project.plugins.apply("io.github.JiangTChen.sonar-quality-gate")

        then:
        project.tasks.findByName("qualityGate") != null
    }
}
