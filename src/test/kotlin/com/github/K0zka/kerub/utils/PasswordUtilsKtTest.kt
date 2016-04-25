package com.github.K0zka.kerub.utils

import org.junit.Test

import org.junit.Assert.*

class PasswordUtilsKtTest {

	@Test
	fun testGenPassword() {
		assertEquals (4, genPassword(4).length)
		assertEquals (8, genPassword(8).length)

		assertEquals(0, genPassword(0).length)
		assertEquals(1, genPassword(1).length)
	}
}