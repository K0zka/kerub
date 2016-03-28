package com.github.K0zka.kerub.model.expectations

import com.fasterxml.jackson.annotation.JsonTypeName
import com.github.K0zka.kerub.model.Expectation
import com.github.K0zka.kerub.model.ExpectationLevel
import com.github.K0zka.kerub.model.io.IoTune

@JsonTypeName("storage-performance")
interface StoragePerformanceExpectation : VirtualStorageExpectation {
	override val level: ExpectationLevel
	val speed: IoTune
}
