package org.keedio.actors.kafka

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.keedio.actors.KafkaProducerActor
import org.keedio.common.message.Stop
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
class KafkaWriterActor extends KafkaProducerActor with LazyLogging {
  override def receive: PartialFunction[Any, Unit] = {
    case SaveTransaction(tx:Transaction) => //txRepo.save(tx)
    case SaveAccount(account:Account) => //accRepo.save(account)
    case DeleteTransaction(tx:Transaction) => //txRepo.delete(tx.getId)
    case Stop() => context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}
