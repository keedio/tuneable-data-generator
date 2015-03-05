package org.keedio.actors.db

import akka.actor.Actor
import org.keedio.common.message.{AckBytes, Stop}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveTransaction}
import org.keedio.domain.{Account, Transaction}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 16/2/15.
 */
@Component("dbWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DBLoggerActor extends Actor{
  val logger = LoggerFactory.getLogger("dbLogger")

  def receive = {
    case SaveTransaction(tx:AnyRef) =>
      val msg = s"${tx.toString}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case DeleteTransaction(tx:AnyRef) =>
      val msg = s"${tx.toString}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case Stop() =>
      logger.debug("Detaching syslog appender")
      context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
