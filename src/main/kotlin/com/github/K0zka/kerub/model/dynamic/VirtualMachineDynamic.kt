package com.github.K0zka.kerub.model.dynamic

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import com.github.K0zka.kerub.model.VirtualMachineStatus
import org.hibernate.search.annotations.DocumentId
import org.hibernate.search.annotations.Field
import java.math.BigInteger
import java.util.UUID
import org.hibernate.search.annotations.Analyze.NO as noAnalyze

@JsonTypeName("vm-dyn")
data class VirtualMachineDynamic(
		@DocumentId
		@JsonProperty("id")
		override
		val id: UUID,
		override val lastUpdated: Long = System.currentTimeMillis(),
		val hostId: UUID,
		@Field
		val status: VirtualMachineStatus = VirtualMachineStatus.Down,
		val memoryUsed: BigInteger,
		val cpuUsage: List<CpuStat> = listOf(),
		val displaySetting : DisplaySettings? = null
) : DynamicEntity {
	//TODO: issue #125 - workaround to allow infinispan query hostId
	@Field(analyze = noAnalyze)
	@JsonIgnore
	fun getHostIdStr() = hostId.toString()
}
