package com.github.kerubistan.kerub.model.dynamic

import io.github.kerubistan.kroki.numbers.sumBy
import org.codehaus.jackson.annotate.JsonTypeName
import java.math.BigInteger
import java.util.UUID

@JsonTypeName("composite")
data class CompositeStorageDeviceDynamic(
		override val id: UUID,
		val reportedFreeCapacity: BigInteger? = null,
		val items: List<CompositeStorageDeviceDynamicItem> = listOf()
) : StorageDeviceDynamic {

	override val freeCapacity: BigInteger = reportedFreeCapacity
			?: items.sumBy(CompositeStorageDeviceDynamicItem::freeCapacity)

	override fun withFreeCapacity(freeCapacity: BigInteger) = this.copy(
			reportedFreeCapacity = freeCapacity
	)


}