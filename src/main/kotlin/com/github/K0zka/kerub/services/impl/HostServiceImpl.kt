package com.github.K0zka.kerub.services.impl

import com.github.K0zka.kerub.data.HostDao
import com.github.K0zka.kerub.host.HostManager
import com.github.K0zka.kerub.host.SshClientService
import com.github.K0zka.kerub.host.getSshFingerPrint
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.model.HostPubKey
import com.github.K0zka.kerub.model.paging.SearchResultPage
import com.github.K0zka.kerub.services.HostAndPassword
import com.github.K0zka.kerub.services.HostService

class HostServiceImpl(
		override val dao: HostDao,
		private val manager: HostManager,
		private val sshClientService: SshClientService)
: ListableBaseService<Host>("host"), HostService {
	override fun getByAddress(address: String): List<Host> = dao.byAddress(address)

	override fun search(field: String, value: String, start: Long, limit: Int): SearchResultPage<Host> =
		dao.fieldSearch(field, value, start, limit).let {
			SearchResultPage(
					start = start,
					count = it.size.toLong(),
					result = it,
					searchby = field,
					total = it.size.toLong()
			)
		}

	override fun getPubkey(): String
			= sshClientService.getPublicKey()

	override fun joinWithoutPassword(host: Host): Host
			= manager.join(host)

	override fun join(hostPwd: HostAndPassword): Host
			= manager.join(hostPwd.host, hostPwd.password)

	override fun getHostPubkey(address: String): HostPubKey {
		val publicKey = manager.getHostPublicKey(address)
		return HostPubKey(publicKey.algorithm, publicKey.format, getSshFingerPrint(publicKey))
	}
}