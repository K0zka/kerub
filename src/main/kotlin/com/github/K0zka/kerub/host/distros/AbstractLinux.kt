package com.github.K0zka.kerub.host.distros

import com.github.K0zka.kerub.data.dynamic.HostDynamicDao
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.model.OperatingSystem
import com.github.K0zka.kerub.model.dynamic.HostDynamic
import com.github.K0zka.kerub.model.dynamic.HostStatus
import com.github.K0zka.kerub.utils.getLogger
import com.github.K0zka.kerub.utils.junix.mpstat.MPStat
import com.github.K0zka.kerub.utils.junix.vmstat.VmStat
import org.apache.sshd.ClientSession
import java.util.UUID

public abstract class AbstractLinux : Distribution {

	override val operatingSystem = OperatingSystem.Linux

	companion object {
		val logger = getLogger(AbstractLinux::class)
	}

	fun doWithDyn(id: UUID, hostDynDao: HostDynamicDao, action: (HostDynamic) -> HostDynamic) {
		val hostDyn = hostDynDao.get(id)
		if (hostDyn == null) {
			val newHostDyn = HostDynamic(
					id = id,
					status = HostStatus.Up
			)
			hostDynDao.add(action(newHostDyn))
		} else {
			hostDynDao.update(action(hostDyn))
		}
	}

	override fun startMonitorProcesses(session: ClientSession, host: Host, hostDynDao: HostDynamicDao) {
		val id = host.id
		MPStat.monitor(session, {
			stats ->
			doWithDyn(id, hostDynDao, {
				it.copy(
						cpuStats = stats
				)
			})
		})
		//TODO: if mpstat is available, vmstat should only update the memory information
		VmStat.vmstat(session, { event ->
			doWithDyn(id, hostDynDao, {
				it.copy(
						status = HostStatus.Up,
						idleCpu = event.idleCpu,
						systemCpu = event.systemCpu,
						userCpu = event.userCpu
				)
			})
		})
	}
}