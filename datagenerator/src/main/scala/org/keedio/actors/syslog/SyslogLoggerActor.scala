package org.keedio.actors.syslog

import akka.actor.Actor
import ch.qos.logback.classic.net.SyslogAppender
import ch.qos.logback.classic.{Logger, LoggerContext}
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.keedio.common.message.{AckBytes, Ack, Start, Stop}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveTransaction}
import org.keedio.domain.{Account, Transaction}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 11/2/15.
 */
@Component("sysloggerActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class SyslogLoggerActor extends Actor {
  val logger = LoggerFactory.getLogger("syslogLogger")

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
