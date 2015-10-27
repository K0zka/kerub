package com.github.K0zka.kerub.planner.costs

import java.util.*

public object IOCostComparator : Comparator<IOCost> {
	override fun compare(first: IOCost, second: IOCost): Int {
		return first.bytes - second.bytes
	}
}