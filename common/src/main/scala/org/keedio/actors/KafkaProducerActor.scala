package org.keedio.actors

import java.util.Properties

import akka.actor.{PoisonPill, Actor}
import com.typesafe.scalalogging.slf4j.LazyLogging
import kafka.javaapi.producer.Producer
import kafka.producer.ProducerConfig
import kafka.producer.ProducerConfig
import kafka.producer.{Partitioner, KeyedMessage, ProducerConfig}
import org.bson.BSONObject
import org.keedio.common.message.{Process, Stop, Ack}
import org.keedio.config.KafkaConfig

/**
 * Processes a BSONDocument.
 */
class KafkaProducerActor extends Actor with LazyLogging with KafkaConfig {

  protected def topic = "mongo.oplog"

  val props = new Properties()

  props.put("metadata.broker.list", kafkaBrokers);
  props.put("serializer.class", serializerClass);
  props.put("key.serializer.class", keySerializerClass);
  props.put("request.required.acks", requiredAcks);

  val producerConfig = new ProducerConfig(props);
  val producer = new Producer[String, BSONObject](producerConfig)

  logger.debug(""+kafkaBrokers)

  logger.info("Kafka producer initialized")

  override def receive = {
    case Process(doc: Some[BSONObject]) => {

      val operationType: String = doc.get.get("op").asInstanceOf[String]
      val id = ""+doc.get.get("h").asInstanceOf[Long]
      val msg = new KeyedMessage(topic, id, doc.get)

      producer.send(msg)

      sender ! Ack
    }
    case Stop => {
      logger.debug("Received StopProcessing")
      logger.debug("Closing Kafka Producer")
      if (producer != null) producer.close

      sender ! Ack
      self ! PoisonPill
    }
    case _ => println("Received unknown message")
  }

}