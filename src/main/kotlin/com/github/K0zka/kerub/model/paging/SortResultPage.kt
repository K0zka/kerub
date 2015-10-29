package com.github.K0zka.kerub.model.paging


public data class SortResultPage<T>(override val start: Long = 0,
									override val count: Long,
									override val total: Long,
									override val result: List<T>,
									val sortBy: String = "id") : ResultPage<T>