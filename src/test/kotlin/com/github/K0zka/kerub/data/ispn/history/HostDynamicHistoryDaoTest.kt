package com.github.K0zka.kerub.data.ispn.history

import com.github.K0zka.kerub.data.ispn.AbstractIspnDaoTest
import com.github.K0zka.kerub.model.dynamic.HostDynamic
import com.github.K0zka.kerub.model.dynamic.HostStatus
import com.github.K0zka.kerub.model.history.HistoryEntry
import com.github.K0zka.kerub.testHost
import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.deferred
import nl.komponents.kovenant.then
import org.infinispan.notifications.Listener
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent
import org.junit.Test
import java.util.UUID
import kotlin.test.assertTrue

class HostDynamicHistoryDaoTest : AbstractIspnDaoTest<UUID, HistoryEntry>() {

	@Listener
	class CreateEventListener(private val deferred : Deferred<Unit, Exception>) {
		@CacheEntryCreated
		fun listen(event: CacheEntryCreatedEvent<UUID, HistoryEntry>) {
			if(!event.isPre) {
				deferred.resolve(Unit)
			}
		}
	}

	@Test
	fun log() {
		val deferred = deferred<Unit, Exception>()
		cache!!.addListener(CreateEventListener(deferred))
		HostDynamicHistoryDao(cache!!).log(
				HostDynamic(id = testHost.id, status = HostStatus.Up, ksmEnabled = false),
				HostDynamic(id = testHost.id, status = HostStatus.Up, ksmEnabled = true)
		)
		deferred.promise.then {
			assertTrue(cache!!.isNotEmpty())
		}.get()
	}

}