package com.github.K0zka.kerub.data.ispn

import com.github.K0zka.kerub.data.EventDao
import com.github.K0zka.kerub.model.Event
import java.util.UUID
import org.infinispan.AdvancedCache
import org.infinispan.query.Search

public class EventDaoImpl(val cache : AdvancedCache<UUID, Event>) : EventDao {
	override fun add(event: Event): UUID {
		val id = UUID.randomUUID()
		event.id = id
		cache.putAsync(id, event)
		return id
	}
	override fun get(id: UUID): Event? {
		return cache.get(id)
	}
	override fun listAll(): List<Event> {
		return Search.getQueryFactory(cache)!!
				.from(javaClass<Event>())!!
				.build()!!
				.list()!!
	}
}