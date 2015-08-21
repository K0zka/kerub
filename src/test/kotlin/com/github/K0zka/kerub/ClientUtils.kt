package com.github.K0zka.kerub

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import com.github.K0zka.kerub.security.mappers.RestError
import com.github.K0zka.kerub.services.getServiceBaseUrl
import com.github.K0zka.kerub.utils.createObjectMapper
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper
import org.apache.cxf.jaxrs.client.WebClient
import java.io.InputStream
import javax.ws.rs.core.Response

class RestException(val msg : String, val code : String, val status : Int, val response : Response) : RuntimeException()

class RestExceptionHandler(val objectMapper : ObjectMapper) : ResponseExceptionMapper<Exception> {
	override fun fromResponse(r: Response?): Exception? {
		val entity = objectMapper.readValue(r!!.getEntity() as InputStream, javaClass<RestError>())
		throw RestException(entity.message!!,entity.code!!,r!!.getStatus(), r)
	}
}

fun createClient() : WebClient {
	val objectMapper = createObjectMapper()
	val client = WebClient.create(getServiceBaseUrl(), listOf(JacksonJsonProvider(objectMapper), RestExceptionHandler(objectMapper)), true)
	WebClient.getConfig(client).getRequestContext().put(
			org.apache.cxf.message.Message.MAINTAIN_SESSION, true)
	return client;
}