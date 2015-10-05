package com.github.K0zka.kerub.planner.steps.vm.start

import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.model.VirtualMachine
import com.github.K0zka.kerub.model.VirtualMachineStatus
import com.github.K0zka.kerub.model.dynamic.HostDynamic
import com.github.K0zka.kerub.model.dynamic.VirtualMachineDynamic
import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.steps.AbstractOperationalStep

public class StartVirtualMachine(val vm: VirtualMachine, val host: Host) : AbstractOperationalStep() {
	override fun take(state: OperationalState): OperationalState {
		val hostDyn = state.hostDyns[host.id] ?: HostDynamic(id = host.id)
		return state.copy(
				vmDyns = state.vmDyns + (vm.id to VirtualMachineDynamic(
						status = VirtualMachineStatus.Up,
						lastUpdated = System.currentTimeMillis(),
						id = vm.id,
						hostId = host.id,
						memoryUsed = vm.memoryMb.min.toLong() * 1024 * 1024
				                                                       )),
				hostDyns = (state.hostDyns - host.id) + (host.id to
						hostDyn.copy(
								idleCpu = hostDyn.idleCpu, // TODO - estimate on the virtual machine CPU usage
								memFreeMb = hostDyn.memFreeMb, //TODO - estimate on memory usage of the VM
								memUsedMb = hostDyn.memUsedMb // TODO + estimate on memory usage of the VM
						            ))
		                 )
	}
}