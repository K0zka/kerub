package com.github.K0zka.kerub.data.ispn

import org.junit.Test

public class IspnConfigurationTest {
	Test
	fun initAndBuild() {
		val config = IspnConfiguration()
		config.clusterName = "test-cluster"
		config.staticOwners = 4
		config.dynamicOwners = 2
		config.baseDir = "test-dir"
		config.rackId = "test-rack"
		config.siteId = "test-site"

		config.init()

		val ispnConfig = config.build()
		val globalConfig = config.buildGlobal()

	}
}