package com.github.K0zka.kerub.services.impl

import com.github.K0zka.kerub.data.HostDao
import com.github.K0zka.kerub.data.VirtualStorageDeviceDao
import com.github.K0zka.kerub.data.dynamic.VirtualStorageDeviceDynamicDao
import com.github.K0zka.kerub.host.HostCommandExecutor
import com.github.K0zka.kerub.model.VirtualStorageDevice
import com.github.K0zka.kerub.model.dynamic.VirtualStorageDeviceDynamic
import com.github.K0zka.kerub.model.dynamic.VirtualStorageFsAllocation
import com.github.K0zka.kerub.model.dynamic.VirtualStorageGvinumAllocation
import com.github.K0zka.kerub.model.dynamic.VirtualStorageLvmAllocation
import com.github.K0zka.kerub.model.expectations.StorageAvailabilityExpectation
import com.github.K0zka.kerub.model.io.VirtualDiskFormat
import com.github.K0zka.kerub.security.AssetAccessController
import com.github.K0zka.kerub.services.VirtualStorageDeviceService
import com.github.K0zka.kerub.utils.junix.qemu.QemuImg
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.scp.ScpTimestamp
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import javax.ws.rs.container.AsyncResponse

class VirtualStorageDeviceServiceImpl(
		dao: VirtualStorageDeviceDao,
		accessController: AssetAccessController,
		private val dynDao: VirtualStorageDeviceDynamicDao,
		private val hostDao: HostDao,
		private val executor: HostCommandExecutor
) : VirtualStorageDeviceService,
		AbstractAssetService<VirtualStorageDevice>(accessController, dao, "virtual disk") {

	override fun add(entity: VirtualStorageDevice): VirtualStorageDevice {
		return accessController.checkAndDo(asset = entity) {
			super.add(entity)
		} ?: entity
	}

	override fun load(id: UUID, type: VirtualDiskFormat, async: AsyncResponse, data: InputStream) {
		val device = getById(id)
		dynDao.waitFor(id) {
			dyn ->
			val host = requireNotNull(hostDao[dyn.allocation.hostId])
			executor.dataConnection(host, {
				session ->
				pump(data, device, dyn, session)

				val virtualSize = if(type != VirtualDiskFormat.raw) {
					val size : Long = QemuImg.info(
							session,
							"${(dyn.allocation as VirtualStorageFsAllocation).mountPoint}/${device.id}"
					).virtualSize
					BigInteger(
							"$size"
					)
				} else {
					device.size
				}

				dao.update(device.copy(
						expectations = device.expectations.filterNot { it is StorageAvailabilityExpectation },
						size = virtualSize
				))
				async.resume(null)
			})

		}

		dao.update(device.copy(
				expectations = device.expectations + StorageAvailabilityExpectation(format = type)
		))
	}

	override fun load(id: UUID, async: AsyncResponse, data: InputStream) {
		load(id, VirtualDiskFormat.raw, async, data)
	}

	private fun pump(data: InputStream, device: VirtualStorageDevice, dyn: VirtualStorageDeviceDynamic, session: ClientSession) {
		when (dyn.allocation) {
			is VirtualStorageLvmAllocation -> {
				uploadRaw(data, device, dyn.allocation.path, session)
			}
			is VirtualStorageGvinumAllocation -> {
				uploadRaw(data, device, "/dev/gvinum/${device.id}", session)
			}
			is VirtualStorageFsAllocation -> {
				uploadRaw(data, device, "${dyn.allocation.mountPoint}/${device.id}", session)
			}
			else -> {
				TODO()
			}
		}
	}

	private fun uploadRaw(data: InputStream, device: VirtualStorageDevice, path: String, session: ClientSession) {
		session.createScpClient().upload(
				data,
				path,
				device.size.toLong(),
				listOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
				ScpTimestamp(System.currentTimeMillis(), System.currentTimeMillis())
		)
	}
}