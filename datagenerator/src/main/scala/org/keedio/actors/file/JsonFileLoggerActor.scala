package org.keedio.actors.file

import akka.actor.Actor
import com.google.gson.Gson
import org.keedio.common.message.{AckBytes, Stop}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveTransaction}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 16/2/15.
 */
@Component("jsonFileWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class JsonFileLoggerActor extends Actor{
  val logger = LoggerFactory.getLogger("fileLogger")

  val gson: Gson = new Gson()

  def receive = {
    case SaveTransaction(tx:AnyRef) =>
      val msg = s"${gson.toJson(tx)}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case DeleteTransaction(tx:AnyRef) =>
      val msg = s"${gson.toJson(tx)}"
      logger.info(msg)
      sender ! AckBytes(msg.getBytes.length)
    case Stop() =>
      logger.debug("Detaching syslog appender")
      context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
