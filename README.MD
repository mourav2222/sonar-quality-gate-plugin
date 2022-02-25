# sonar-quality-gate-plugin

[![Build](https://github.com/JiangTChen/sonar-quality-gate-plugin/workflows/CI/badge.svg)](https://github.com/JiangTChen/sonar-quality-gate-plugin/actions/workflows/CI.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A gradle plugin to checks whether the quality meets the requirements of the specified quality gate

## Usage
build.gradle
```groovy
plugins {
    id 'io.github.JiangTChen.sonar-quality-gate' version '0.1.0'
}

qualityGate {
    gateName = 'Grade1'  //Optional
    intervalSeconds = 2
    retryTimes =15
}
```
gradle.properties
```properties
systemProp.sonar.login = ****
```

Run
```shell
./gradlew qualityGate
```

Execute sonarqube plugin will generate build/sonar/report-task.txt.
Quality gate plugin will take ceTaskUrl, serverUrl, projectKey from report-task.txt
Quality gate plugin get authentication by systemProp.sonar.login.
