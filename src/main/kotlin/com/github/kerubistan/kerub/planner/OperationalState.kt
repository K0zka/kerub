package com.github.kerubistan.kerub.planner

import com.github.kerubistan.kerub.model.Entity
import com.github.kerubistan.kerub.model.Expectation
import com.github.kerubistan.kerub.model.ExpectationLevel
import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.InternalExpectation
import com.github.kerubistan.kerub.model.VirtualMachine
import com.github.kerubistan.kerub.model.VirtualMachineStatus
import com.github.kerubistan.kerub.model.VirtualStorageDevice
import com.github.kerubistan.kerub.model.WorkingHostExpectation
import com.github.kerubistan.kerub.model.collection.DataCollection
import com.github.kerubistan.kerub.model.collection.HostDataCollection
import com.github.kerubistan.kerub.model.collection.VirtualMachineDataCollection
import com.github.kerubistan.kerub.model.collection.VirtualStorageDataCollection
import com.github.kerubistan.kerub.model.config.HostConfiguration
import com.github.kerubistan.kerub.model.controller.config.ControllerConfig
import com.github.kerubistan.kerub.model.dynamic.DynamicEntity
import com.github.kerubistan.kerub.model.dynamic.HostDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualMachineDynamic
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageDeviceDynamic
import com.github.kerubistan.kerub.model.expectations.CacheSizeExpectation
import com.github.kerubistan.kerub.model.expectations.ChassisManufacturerExpectation
import com.github.kerubistan.kerub.model.expectations.ClockFrequencyExpectation
import com.github.kerubistan.kerub.model.expectations.CoreDedicationExpectation
import com.github.kerubistan.kerub.model.expectations.CpuArchitectureExpectation
import com.github.kerubistan.kerub.model.expectations.MemoryClockFrequencyExpectation
import com.github.kerubistan.kerub.model.expectations.NoMigrationExpectation
import com.github.kerubistan.kerub.model.expectations.NotSameHostExpectation
import com.github.kerubistan.kerub.model.expectations.NotSameStorageExpectation
import com.github.kerubistan.kerub.model.expectations.StorageAvailabilityExpectation
import com.github.kerubistan.kerub.model.expectations.VirtualMachineAvailabilityExpectation
import com.github.kerubistan.kerub.model.expectations.VirtualStorageExpectation
import com.github.kerubistan.kerub.model.expectations.internals.NoGarbageExpectation
import com.github.kerubistan.kerub.planner.reservations.Reservation
import com.github.kerubistan.kerub.planner.reservations.VirtualStorageReservation
import com.github.kerubistan.kerub.planner.reservations.VmReservation
import com.github.kerubistan.kerub.utils.join
import com.github.k0zka.finder4j.backtrack.State
import java.util.UUID

data class OperationalState(
		val hosts: Map<UUID, HostDataCollection> = mapOf(),
		val vms: Map<UUID, VirtualMachineDataCollection> = mapOf(),
		val vStorage: Map<UUID, VirtualStorageDataCollection> = mapOf(),
		val reservations: List<Reservation<*>> = listOf(),
		val controllerConfig: ControllerConfig = ControllerConfig()
) : State {

	companion object {

		fun <T : Entity<I>, I> mapById(entities: List<T>): Map<I, T>
				= entities.associateBy { it.id }

		fun mapHostData(hosts: List<Host> = listOf(),
						hostDyns: List<HostDynamic> = listOf(),
						hostCfgs: List<HostConfiguration> = listOf()): Map<UUID, HostDataCollection> {
			val hostDynMap = mapById(hostDyns)
			val hostCfgMap = mapById(hostCfgs)
			return hosts.map { it.id to HostDataCollection(it, hostDynMap[it.id], hostCfgMap[it.id]) }.toMap()
		}

		fun <I, T : Entity<I>, D : DynamicEntity, C : DataCollection<T, D>>
				mapToCollection(staticData: List<T>, dynamicData: List<D>, transform: (static: T, dynamic: D?) -> C): Map<I, C> {
			val dynMap: Map<UUID, D> = mapById(dynamicData)
			return staticData.map { it.id to transform(it, dynMap[it.id as UUID]) }.toMap()
		}

		fun fromLists(hosts: List<Host> = listOf(),
					  hostDyns: List<HostDynamic> = listOf(),
					  hostCfgs: List<HostConfiguration> = listOf(),
					  vms: List<VirtualMachine> = listOf(),
					  vmDyns: List<VirtualMachineDynamic> = listOf(),
					  vStorage: List<VirtualStorageDevice> = listOf(),
					  vStorageDyns: List<VirtualStorageDeviceDynamic> = listOf(),
					  reservations: List<Reservation<*>> = listOf(),
					  config: ControllerConfig = ControllerConfig()
		) =
				OperationalState(
						hosts = mapHostData(hosts, hostDyns, hostCfgs),
						vms = mapToCollection(vms, vmDyns) { stat, dyn -> VirtualMachineDataCollection(stat, dyn) },
						vStorage = mapToCollection(vStorage, vStorageDyns) { stat, dyn -> VirtualStorageDataCollection(stat, dyn) },
						reservations = reservations,
						controllerConfig = config
				)
	}

	fun vmDataOnHost(hostId: UUID): List<VirtualMachineDataCollection> =
			vms.values
					.filter { it.dynamic?.status == VirtualMachineStatus.Up && it.dynamic.hostId == hostId }

	fun vmsOnHost(hostId: UUID): List<VirtualMachine> =
			vms.values
					.filter { it.dynamic?.status == VirtualMachineStatus.Up && it.dynamic.hostId == hostId }
					.map { vms[it.dynamic!!.id]?.stat }.filterNotNull()

	fun isVmRunning(vm: VirtualMachine): Boolean {
		val dyn = vms[vm.id]?.dynamic
		return dyn != null && dyn.status == VirtualMachineStatus.Up
	}

	fun vmHost(vm: VirtualMachine): Host? = vmHost(vm.id)

	fun vmHost(vmId: UUID): Host? {
		val dyn = vms[vmId]?.dynamic
		return if (dyn == null) null else hosts[dyn.hostId]?.stat
	}

	override fun isComplete(): Boolean {
		//check that all virtual resources has all DealBreaker satisfied
		return vmsToCheck().all {
			(stat) ->
			stat.expectations.all {
				expectation ->
				expectation.level != ExpectationLevel.DealBreaker
						|| isExpectationSatisfied(expectation, stat)
			}
		} && virtualStorageToCheck().all {
			virtualStorageDevice ->
			virtualStorageDevice.expectations.all {
				expectation ->
				expectation.level != ExpectationLevel.DealBreaker
						||
						isExpectationSatisfied(expectation, virtualStorageDevice)
			}
		}
	}

	private fun isExpectationSatisfied(expectation: VirtualStorageExpectation, virtualStorage: VirtualStorageDevice): Boolean {
		when (expectation) {
			is StorageAvailabilityExpectation -> {
				//if storage dynamic exists, allocation must exist
				return vStorage[virtualStorage.id]?.dynamic != null
			}
			is NotSameStorageExpectation -> {
				val diskDyn = vStorage[virtualStorage.id]?.dynamic
				return diskDyn == null || expectation.otherDiskId.let {
					otherVdiskId ->
					val otherDiskDyn = vStorage[otherVdiskId]?.dynamic
					if (otherDiskDyn == null) {
						true
					} else {
						otherDiskDyn.allocation.hostId != diskDyn.allocation.hostId
					}
				}
			}
			else -> return true
		}
	}

	fun virtualStorageToCheck(): List<VirtualStorageDevice> =
			vStorage.values.filterNot {
			reservations.contains(VirtualStorageReservation(it.stat))
		}.map { it.stat }

	fun getNrOfUnsatisfiedExpectations(level: ExpectationLevel): Int =
			getUnsatisfiedExpectations().count { it.level == level }

	internal fun isVmOnRecyclingHost(vm: VirtualMachineDataCollection) =
			hosts[vm.dynamic?.hostId]?.stat?.recycling == true

	fun getUnsatisfiedExpectations(): List<Expectation> =
			vmsToCheck()
					.map {
						vm ->

						vm.stat.expectations.filterNot {
							expectation ->
							isExpectationSatisfied(expectation, vm.stat)
						} + if (isVmOnRecyclingHost(vm)) {
							listOf(WorkingHostExpectation())
						} else {
							listOf()
						}
					}.join() +
					virtualStorageToCheck().map {
						vdisk ->
						vdisk.expectations.filterNot {
							expectation ->
							isExpectationSatisfied(expectation, vdisk)
						}
					}.join() +
					unsatisfiedInternalExpectations()

	private fun unsatisfiedInternalExpectations(): Iterable<InternalExpectation>
			= hosts.filter { it.value.stat.recycling }.map { NoGarbageExpectation(host = it.value.stat) }

	private fun vmsToCheck(): List<VirtualMachineDataCollection> {
		return vms.values
				.filterNot {
					reservations.contains(VmReservation(it.stat))
				}
	}

	internal fun isExpectationSatisfied(expectation: Expectation, vm: VirtualMachine): Boolean {
		when (expectation) {
			is ClockFrequencyExpectation -> {
				val host = vmHost(vm)
				return if (host == null) {
					true
				} else {
					host.capabilities?.cpus?.firstOrNull()?.maxSpeedMhz ?: 0 >= expectation.minimalClockFrequency
				}
			}
			is NotSameHostExpectation -> {
				val host = vmHost(vm)
				return if (host == null) {
					true
				} else {
					val otherVmHosts = vmHost(expectation.otherVmId)?.id
					otherVmHosts != host.id
				}
			}
			is ChassisManufacturerExpectation -> {
				val host = vmHost(vm)
				return if (host == null) {
					true
				} else {
					host.capabilities?.chassis?.manufacturer == expectation.manufacturer
				}
			}
			is CoreDedicationExpectation -> {
				return vmHost(vm)?.let { host ->
					val vmsOnHost = lazy { vmDataOnHost(host.id) }
					val hostCoreCnt = lazy { host.capabilities?.cpus?.sumBy { it.coreCount ?: 0 } ?: 0 }
					val coredDedicated: (VirtualMachineDataCollection) -> Boolean
							= { it.stat.expectations.any { it is CoreDedicationExpectation } }
					val vmNrOfCpus: (VirtualMachineDataCollection) -> Int = { it.stat.nrOfCpus }

					// if this vm has CPU affinity to a smaller nr of cores, than the number of vcpus, that
					// means this expectation is not met... however I would say that may be true even with
					// non-dedicated vcpus
					// to be on the safe side, let's check and break this expectation if so
					vm.nrOfCpus <= requireNotNull(vms[vm.id]).dynamic?.coreAffinity?.size ?: hostCoreCnt.value
							&&
							//first case: under-utilization
							//the total of vcpus on the server is less (or equal if this is the only vm on host)
							//to the cores in the host -> no further enforcement needed, it is fine
							vmsOnHost.value.sumBy(vmNrOfCpus) <= hostCoreCnt.value
							||
							//second case: over-allocation
							// the vm's without core-dedication are stick to a number of cores
							// so that the ones with core-dedication have enough cores left
							vmsOnHost.value.filterNot(coredDedicated)
									.map { it.dynamic?.coreAffinity ?: listOf() }
									.join().toSet().size +
									vmsOnHost.value.filter(coredDedicated)
											.sumBy(vmNrOfCpus) < hostCoreCnt.value
				} == true
			}
			is CacheSizeExpectation -> {
				val host = vmHost(vm)
				return if (host == null) {
					true
				} else {
					expectation.minL1 <= host.capabilities?.cpus?.firstOrNull()?.l1cache?.size ?: 0
				}
			}
			is VirtualMachineAvailabilityExpectation ->
				return isVmRunning(vm) == expectation.up
			is CpuArchitectureExpectation -> {
				val host = vmHost(vm)
				return if (host == null) {
					true
				} else {
					expectation.cpuArchitecture == host.capabilities?.cpuArchitecture
				}
			}
			is MemoryClockFrequencyExpectation -> {
				val host = vmHost(vm)
				val memoryDevices = host?.capabilities?.memoryDevices
				return memoryDevices?.isNotEmpty() == true
						&& memoryDevices.all { it.speedMhz ?: 0 >= expectation.min }
			}
			is NoMigrationExpectation -> return true
			else ->
				throw IllegalArgumentException("Expectation $expectation can not be checked")
		}
	}
}