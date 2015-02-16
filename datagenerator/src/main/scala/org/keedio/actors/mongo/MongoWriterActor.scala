package org.keedio.actors.mongo

import akka.actor.Actor
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.log4j.Logger
import org.keedio.common.message.Stop
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveAccount, SaveTransaction}
import org.keedio.domain.{Account, Transaction}
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component("mongoWriterActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class MongoWriterActor extends Actor with LazyLogging {
  /*
  @Autowired
  var accRepo: AccountRepository = _

  @Autowired
  var txRepo: TransactionRepository = _
  */

  def receive = {
    case SaveTransaction(tx:Transaction) => //txRepo.save(tx)
    case SaveAccount(account:Account) => //accRepo.save(account)
    case DeleteTransaction(tx:Transaction) => //txRepo.delete(tx.getId)
    case Stop() => context stop self
    case _ => logger.error("es.care.sf.business.common.message.Message not recognized")
  }
}