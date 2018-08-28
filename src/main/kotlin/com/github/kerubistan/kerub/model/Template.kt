package com.github.kerubistan.kerub.model

import java.util.UUID
import kotlin.reflect.KClass

/**
 * A template of virtual machines.
 */
data class Template(
		override val id: UUID = UUID.randomUUID(),
		override val name: String,
		override val owner: AssetOwner? = null,
		val vmNamePrefix: String,
		val vm: VirtualMachine
) : Entity<UUID>, Named, Asset {
	override fun references(): Map<KClass<out Asset>, List<UUID>> = vm.references()
}