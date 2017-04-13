package com.github.K0zka.kerub.services.impl

import com.github.K0zka.kerub.RestException
import com.github.K0zka.kerub.createClient
import com.github.K0zka.kerub.expect
import com.github.K0zka.kerub.login
import com.github.K0zka.kerub.model.Host
import com.github.K0zka.kerub.services.HostAndPassword
import com.github.K0zka.kerub.services.HostService
import com.github.K0zka.kerub.services.LoginService
import com.github.K0zka.kerub.testHost
import org.apache.cxf.jaxrs.client.JAXRSClientFactory
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test
import java.util.UUID

class HostServiceIT {

	@Test
	fun security() {
		//unauthenticated
		checkNoAccess(JAXRSClientFactory.fromClient(createClient(),
				HostService::class.java, true))

		//end user has nothing to do with hosts
		val endUserClient = createClient()
		endUserClient.login("enduser","password")
		checkNoAccess(JAXRSClientFactory.fromClient(endUserClient,
				HostService::class.java, true))

	}

	private fun checkNoAccess(service: HostService) {
		expect(
				clazz = RestException::class,
				action = { service.search(Host::address.name, "any", 0, Int.MAX_VALUE) },
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = { service.getByAddress("example.com") },
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = { service.joinWithoutPassword(testHost) },
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = {
					service.join(
							HostAndPassword(host = testHost, password = "secret")
					)
				},
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = { service.getById(UUID.randomUUID()) },
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = { service.delete(testHost.id) },
				check = { it.code == "AUTH1" }
		)

		expect(
				clazz = RestException::class,
				action = { service.listAll(start = 0, limit = 100, sort = Host::address.name) },
				check = { it.code == "AUTH1" }
		)
	}

	//TODO: issue #127 make a cucumber story out of this
	@Test
	fun getPubkey() {
		val client = createClient()
		val login = JAXRSClientFactory.fromClient(client, LoginService::class.java, true)
		login.login(LoginService.UsernamePassword(username = "admin", password = "password"))
		val service = JAXRSClientFactory.fromClient(client,
				HostService::class.java, true)
		Assert.assertThat(service.getPubkey(), CoreMatchers.notNullValue())
	}

}