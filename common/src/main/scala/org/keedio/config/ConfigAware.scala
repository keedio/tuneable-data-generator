package org.keedio.config

import akka.actor.ActorSystem
import com.typesafe.config._

/**
 * Created by luca on 13/11/14.
 */
trait ConfigAware {
  val keedioConfig = ConfigFactory.load()

  val ackTimeout = keedioConfig.getLong("ack.timeout")
  val reportInterval = keedioConfig.getLong("report.interval")


}
