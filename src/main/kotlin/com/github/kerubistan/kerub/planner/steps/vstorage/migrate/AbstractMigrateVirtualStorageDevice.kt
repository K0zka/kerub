package com.github.kerubistan.kerub.planner.steps.vstorage.migrate

import com.github.kerubistan.kerub.model.Host
import com.github.kerubistan.kerub.model.VirtualStorageDevice
import com.github.kerubistan.kerub.model.dynamic.VirtualStorageAllocation
import com.github.kerubistan.kerub.planner.OperationalState
import com.github.kerubistan.kerub.planner.reservations.HostStorageReservation
import com.github.kerubistan.kerub.planner.reservations.Reservation
import com.github.kerubistan.kerub.planner.reservations.UseHostReservation
import com.github.kerubistan.kerub.planner.reservations.VirtualStorageReservation
import com.github.kerubistan.kerub.planner.steps.AbstractOperationalStep
import com.github.kerubistan.kerub.utils.update

abstract class AbstractMigrateVirtualStorageDevice : AbstractOperationalStep {

	abstract val device: VirtualStorageDevice
	abstract val sourceAllocation: VirtualStorageAllocation
	abstract val targetAllocation: VirtualStorageAllocation
	abstract val source: Host
	abstract val target: Host

	override fun reservations(): List<Reservation<*>> = listOf(
			VirtualStorageReservation(device),
			HostStorageReservation(target, device.size),
			UseHostReservation(source),
			UseHostReservation(target)
	)

	override fun take(state: OperationalState): OperationalState = state.copy(
			vStorage = state.vStorage.update(device.id) { vStorageColl ->
				vStorageColl.copy(
						dynamic = vStorageColl.dynamic!!.copy(
								allocations = vStorageColl.dynamic.allocations - sourceAllocation + targetAllocation
						)
				)
			}
	)
}