package com.github.K0zka.kerub.data.ispn

import com.github.K0zka.kerub.utils.getLogger
import org.infinispan.configuration.cache.Configuration
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder
import org.infinispan.configuration.global.GlobalConfiguration
import org.infinispan.configuration.parsing.ParserRegistry

class IspnConfiguration {
	var template = "infinispan.xml"
	var baseDir = "."
	var dynamicOwners = 1
	var staticOwners = 2
	var clusterName = "kerub"
	var rackId = "default-rack"
	var siteId = "default-site"

	companion object {
		val logger = getLogger(IspnConfiguration::class)
	}

	private var globalConfig : GlobalConfiguration? = null
	private var config : Configuration? = null

	fun init() {
		logger.info("ispn global configuration")
		logger.info("site id: {}", siteId)
		logger.info("cluster name: {}", clusterName)
		logger.info("rack id: {}", rackId)
		logger.info("storage directory: {}", baseDir)
		logger.info("static owners: {}", staticOwners)
		logger.info("dynamic owners: {}", dynamicOwners)



		//TODO: setting system property to configure ISPN is highly unfriendly
		// alternative needs to be investigated
		System.setProperty("store.dir", baseDir)
		System.setProperty("dyn.owners", dynamicOwners.toString())
		System.setProperty("stat.owners", staticOwners.toString())

		val template = loadTemplate()
		var globalConfigBuilder = template.getGlobalConfigurationBuilder()

		globalConfigBuilder.transport().clusterName(clusterName).rackId(rackId).siteId(siteId)

		var configBuilder = template.getCurrentConfigurationBuilder()
		globalConfig = globalConfigBuilder.build()
		config = configBuilder.build(globalConfig)
	}

	fun build() : Configuration {
		return config!!
	}

	fun buildGlobal() : GlobalConfiguration {
		return globalConfig!!
	}

	fun loadTemplate() =
		Thread.currentThread().getContextClassLoader().getResourceAsStream(template).use {
			ParserRegistry().parse(it)
		}

}