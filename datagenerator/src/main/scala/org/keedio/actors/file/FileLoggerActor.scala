package org.keedio.actors.file

import java.nio.charset.Charset

import akka.actor.Actor
import org.keedio.common.message.{AckBytes, Ack, Stop}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveAccount, SaveTransaction}
import org.keedio.domain.{Account, Transaction}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 16/2/15.
 */
@Component("fileWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FileLoggerActor extends Actor{
  val logger = LoggerFactory.getLogger("fileLogger")

  def receive = {
    case SaveTransaction(tx:Transaction) =>
      val msg = s"SaveTransaction: ${tx.toString}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case SaveAccount(account:Account) =>
      val msg = s"SaveAccount: ${account.toString}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case DeleteTransaction(tx:Transaction) =>
      val msg = s"DeleteTransaction: ${tx.toString}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case Stop() =>
      logger.debug("Detaching syslog appender")
      context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
