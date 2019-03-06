package com.github.kerubistan.kerub.planner.steps.host.security.install

import com.github.kerubistan.kerub.data.config.HostConfigurationDao
import com.github.kerubistan.kerub.host.HostCommandExecutor
import com.github.kerubistan.kerub.model.config.HostConfiguration
import com.github.kerubistan.kerub.planner.execution.AbstractStepExecutor
import com.github.kerubistan.kerub.utils.getLogger
import com.github.kerubistan.kerub.utils.junix.ssh.openssh.OpenSsh

class InstallPublicKeyExecutor(private val hostCommandExecutor: HostCommandExecutor,
							   private val hostCfgDao: HostConfigurationDao)
	: AbstractStepExecutor<InstallPublicKey, Unit>(){

	private val logger = getLogger(InstallPublicKeyExecutor::class)

	override fun perform(step: InstallPublicKey) {
		logger.info("installing public key on target host {}", step.targetHost.address)
		hostCommandExecutor.execute(step.targetHost) {
			OpenSsh.authorize(it, step.publicKey)
		}
		logger.info("adding known host {} on host {}", step.targetHost.address, step.sourceHost.address)
		hostCommandExecutor.execute(step.sourceHost) {
			OpenSsh.addKnownHost(it, step.targetHost.address, step.targetHost.publicKey)
		}
	}

	override fun update(step: InstallPublicKey, updates: Unit) {
		hostCfgDao.updateWithDefault(step.targetHost.id, defaultValue = { HostConfiguration(id = step.targetHost.id) }) {
			it.copy(
					acceptedPublicKeys = it.acceptedPublicKeys + step.publicKey
			)
		}
	}

}