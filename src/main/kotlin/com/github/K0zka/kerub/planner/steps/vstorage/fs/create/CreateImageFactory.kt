package com.github.K0zka.kerub.planner.steps.vstorage.fs.create

import com.github.K0zka.kerub.model.FsStorageCapability
import com.github.K0zka.kerub.model.expectations.StorageAvailabilityExpectation
import com.github.K0zka.kerub.model.io.VirtualDiskFormat
import com.github.K0zka.kerub.planner.OperationalState
import com.github.K0zka.kerub.planner.steps.vstorage.AbstractCreateVirtualStorageFactory
import com.github.K0zka.kerub.utils.junix.qemu.QemuImg

object CreateImageFactory : AbstractCreateVirtualStorageFactory<CreateImage>() {
	override fun produce(state: OperationalState): List<CreateImage> {
		val storageNotAllocated = listStorageNotAllocated(state)
		val runningHosts = listRunningHosts(state)

		var steps = listOf<CreateImage>()

		val storageTechnologies = state.controllerConfig.storageTechnologies
		runningHosts.filter { QemuImg.available(it.stat.capabilities) }
				.forEach {
					hostData ->
					hostData.stat.capabilities?.storageCapabilities?.filter {
						capability
						->
						capability is FsStorageCapability
								&& capability.mountPoint in storageTechnologies.fsPathEnabled
								&& capability.fsType in storageTechnologies.fsTypeEnabled
					}?.forEach {
						mount ->
						steps += storageNotAllocated.map {
							storage ->
							val format =
									(storage.expectations.firstOrNull { it is StorageAvailabilityExpectation }
											as StorageAvailabilityExpectation?)?.format
											?: VirtualDiskFormat.qcow2
							CreateImage(
									disk = storage,
									host = hostData.stat,
									format = format,
									path = (mount as FsStorageCapability).mountPoint
							)
						}
					}
				}

		return steps
	}


}