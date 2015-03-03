package org.keedio.actors.kafka

import java.util.{Properties, UUID}

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.LazyLogging
import kafka.producer.{Producer, ProducerConfig, KeyedMessage}
import org.bson.BSONObject
import org.keedio.actors.KafkaProducerActor
import org.keedio.common.message.{AckBytes, Ack, Stop}
import org.keedio.config.KafkaConfig
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveAccount, SaveTransaction}
import org.keedio.domain.{Account, Transaction}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 16/2/15.
 */
@Component("kafkaWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class KafkaWriterActor extends Actor with LazyLogging with KafkaConfig{

  val props = new Properties()

  props.put("metadata.broker.list", kafkaBrokers);
  props.put("serializer.class", "kafka.serializer.StringEncoder");
  props.put("key.serializer.class", "kafka.serializer.StringEncoder");
  props.put("request.required.acks", requiredAcks);

  val producerConfig = new ProducerConfig(props);
  val producer = new Producer[String, String](producerConfig)

  override def receive: PartialFunction[Any, Unit] = {
    case SaveTransaction(tx:Transaction) => {
      val msg = new KeyedMessage[String,String](topic, UUID.randomUUID().toString,tx.toString)
      producer.send(msg)
      sender ! AckBytes(msg.message.getBytes.length)
    }
    case SaveAccount(account:Account) => {
      val msg = new KeyedMessage[String,String](topic, UUID.randomUUID().toString,account.toString)
      producer.send(msg)
      sender ! AckBytes(msg.message.getBytes.length)
    }
    case DeleteTransaction(tx:Transaction) => {
      val msg = new KeyedMessage[String,String](topic, UUID.randomUUID().toString,tx.toString)
      producer.send(msg)
      sender ! AckBytes(msg.message.getBytes.length)
    }
    case Stop() => context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }

  protected def topic: String = kafkaTopic
}
