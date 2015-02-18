package org.keedio.config

import scala.collection.JavaConversions._

/**
 * Created by luca on 26/11/14.
 */
trait KafkaConfig extends ConfigAware {
  val kafkaBrokers = keedioConfig.getString("kafka.brokers")

  val kafkaTopic = keedioConfig.getString("kafka.topic")
  val serializerClass = keedioConfig.getString("kafka.serializer.class")
  val keySerializerClass = keedioConfig.getString("kafka.key.serializer.class")
  val requiredAcks = if (keedioConfig.getBoolean("kafka.request.required.acks")) "1" else "0"
}
