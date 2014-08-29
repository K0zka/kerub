package com.github.K0zka.kerub.utils.activeobject

import org.junit.Test
import org.mockito.Mock
import org.springframework.jms.core.JmsTemplate
import org.junit.runner.RunWith
import org.mockito.runners.MockitoJUnitRunner
import org.mockito.Mockito
import org.mockito.Matchers
import org.springframework.jms.core.MessageCreator
import javax.jms.Session

RunWith(javaClass<MockitoJUnitRunner>())
public class MqInvocationQueueTest {
	Mock
	var template : JmsTemplate? = null
	Mock
	var session : Session? = null
	Test
	fun send() {
		val queue = MqInvocationQueue(template!!)
		Mockito.doAnswer({ (it!!.getArguments()!![0] as MessageCreator).createMessage(session) })!!
				.`when`(template)!!.send(Matchers.any(javaClass<MessageCreator>()))
		queue.send(AsyncInvocation("", "", listOf(), listOf()))
		Mockito.verify(template)!!.send(Matchers.any(javaClass<MessageCreator>()));
		Mockito.verify(session)!!.createObjectMessage(Matchers.any(javaClass<AsyncInvocation>()))
	}
}