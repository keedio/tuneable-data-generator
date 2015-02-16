package org.keedio.actors.file

import akka.actor.Actor
import org.keedio.common.message.Stop
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
    case SaveTransaction(tx:Transaction) => logger.info(s"SaveTransaction: ${tx.toString}")
    case SaveAccount(account:Account) => logger.info(s"SaveAccount: ${account.toString}")
    case DeleteTransaction(tx:Transaction) => logger.info(s"DeleteTransaction: ${tx.toString}")
    case Stop() =>
      logger.debug("Detaching syslog appender")
      context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
