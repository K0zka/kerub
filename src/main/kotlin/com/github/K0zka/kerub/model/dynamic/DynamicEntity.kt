package com.github.K0zka.kerub.model.dynamic

import com.github.K0zka.kerub.model.Entity
import java.util.UUID

data interface DynamicEntity : Entity<UUID> {
	val lastUpdated: Long

}