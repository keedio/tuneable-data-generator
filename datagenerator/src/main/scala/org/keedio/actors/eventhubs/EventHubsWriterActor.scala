package org.keedio.actors.eventhubs

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.LazyLogging
import kafka.producer.KeyedMessage
import org.apache.qpid.amqp_1_0.jms.{Session, ConnectionFactory, Destination}
import org.keedio.common.message.{AckBytes, Stop}
import org.keedio.config.EventHubsConfig
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveTransaction}
import org.keedio.datagenerator.jndi.HashMapInitialContextFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import scala.collection.JavaConversions._

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 16/2/15.
 */
@Component("eventHubsWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class EventHubsWriterActor extends Actor with LazyLogging with EventHubsConfig {

  val ctx = new HashMapInitialContextFactory().getInitialContext(environment)
  val cf = ctx.lookup("SBCF").asInstanceOf[ConnectionFactory]
  val queue = ctx.lookup("KEEDIO").asInstanceOf[Destination]
  val connection = cf.createConnection
  connection.start()
  val sendSession = connection.createSession(false, 1)
  val producer = sendSession.createProducer(queue)


  override def receive: PartialFunction[Any, Unit] = {
    case SaveTransaction(tx:AnyRef) => {
      val msg = sendSession.createTextMessage(tx.toString)
      producer.send(msg)
      sender ! AckBytes(tx.toString.length)
    }
    case DeleteTransaction(tx:AnyRef) => {
      val msg = sendSession.createTextMessage(tx.toString)
      producer.send(msg)
      sender ! AckBytes(tx.toString.length)
    }
    case Stop() =>
      sendSession.close()
      connection.stop()
      connection.close()
      context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
