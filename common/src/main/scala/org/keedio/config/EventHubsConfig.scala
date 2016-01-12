package org.keedio.config

import java.util
import java.util.Map.Entry

import com.typesafe.config.ConfigValue
import scala.collection.JavaConversions._

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 12/1/16.
 */
trait EventHubsConfig extends ConfigAware {

  val iter = keedioConfig.entrySet().iterator()

  val eventHubProps: Map[String, String] = (for (e: Entry[String,ConfigValue] <- iter
       if e.getKey.startsWith(EventHubsConfig.CONNECTION_FACTORY_PREFIX)) yield { (e.getKey,e.getValue.unwrapped().asInstanceOf[String]) }).toMap[String, String]

  val environment = new java.util.Hashtable[String, String]
  eventHubProps.foreach(e => environment.put(e._1, e._2) )
}

object EventHubsConfig{
  val CONNECTION_FACTORY_PREFIX = "event.hubs."

}


