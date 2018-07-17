package com.github.kerubistan.kerub.planner.steps.host.security.remove

import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStepFactory
import com.github.kerubistan.kerub.utils.times

object RemovePublicKeyFactory : AbstractOperationalStepFactory<RemovePublicKey>() {
	override fun produce(state: OperationalState): List<RemovePublicKey> = state.runningHosts.let { runningHosts ->
		(runningHosts * runningHosts).filter { (host, keyHost) ->
			keyHost.config?.publicKey != null
					&& keyHost.stat.id != host.stat.id
					&& host.config?.acceptedPublicKeys != null
					&& keyHost.config.publicKey in host.config.acceptedPublicKeys
		}.map { (host, keyHost) ->
			RemovePublicKey(host = host.stat, hostOfKey = keyHost.stat, publicKey = keyHost.config?.publicKey!!)
		}
	}
}