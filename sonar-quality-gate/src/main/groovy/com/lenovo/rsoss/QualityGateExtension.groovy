package com.lenovo.rsoss

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class QualityGateExtension {
    @Input
    @Optional
    abstract Property<String> getGateName()

    @Input
    abstract Property<Integer> getRetryTimes()

    @Input
    abstract Property<Integer> getIntervalSeconds()
}
