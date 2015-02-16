package org.keedio.actors

import java.text.DecimalFormat
import javax.annotation.PostConstruct

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import com.google.common.util.concurrent.RateLimiter
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.keedio.common.message.{Process, Start, Stop}
import org.keedio.datagenerator.config.{DataGeneratorConfigAware, SpringActorProducer}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveAccount, SaveTransaction}
import org.keedio.datagenerator.{RandomAccountGenerator, RandomAccountTransactionGenerator}
import org.keedio.domain.AccountTransaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Random

object RandomGeneratorActor {

}

@Component("randomGeneratorActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RandomGeneratorActor() extends Actor with LazyLogging with DataGeneratorConfigAware {

  var INSERT_COUNT: Int = 0
  var UPDATE_COUNT: Int = 0
  var DELETE_COUNT: Int = 0
  var OPERATIONS: Int = 0
  var OPERATIONS_SKIPPED: Int = 0
  var START_TIME: Long = _
  var lastOutput: Long = _

  val longFmt = new DecimalFormat("###,###")

  @Autowired
  var ctx: ApplicationContext = _

  var writer: ActorRef = _

  var rateLimiter: RateLimiter = _

  val TIMEOUT = Duration(30, SECONDS)

  implicit val timeout = Timeout(30 seconds)

  @PostConstruct
  def init(): Unit = {
    writer = context.actorOf(Props(classOf[SpringActorProducer], ctx, activeActor), activeActor)

    logger.info(s"Configuring rate limiter with $limitVal permits per second")
    rateLimiter = RateLimiter.create(limitVal)
  }

  def receive = {
    case Start() =>
      logger.info("received start message")
      START_TIME = System.currentTimeMillis()
      writer ! Start()
      doGenerateData()
    case Process(e) =>
      doGenerateData()
    case e: Stop => {
      logger.warn("received stop message")
      context stop self
    }
    case e:Any => logger.error(s"Unknown message of type: ${e.getClass}")
  }

  def doGenerateData(): Unit = {
    val r = Random

    val accountGenerator = RandomAccountGenerator()
    val txGenerator = RandomAccountTransactionGenerator()

    val numTxs = r.nextInt(numTxsPerAccount) + 1

    val account = accountGenerator.generate()
    writer ! SaveAccount(account.get)
    /*val accountFuture = writer ? SaveAccount(account.get)
    Await.ready(accountFuture, TIMEOUT)

    accountFuture onSuccess {
      case rc:Option[_] =>
    }
    */

    var txCount = 0

    var txs = mutable.MutableList[AccountTransaction]()

    val numDeletes: Int = Math.floor(numTxs * deleteRatio).asInstanceOf[Int]
    val numUpdates: Int = Math.floor(numTxs * updateRatio).asInstanceOf[Int]
    val numInserts: Int = numTxs - numUpdates - numDeletes

    logger.debug(s"Account: ${account.get.ccc}: $numTxs events will be generated ($numInserts inserts, $numUpdates updates, $numDeletes deletes) ")

    for (idx <- 1 to numTxs) {
      rateLimiter.acquire()

      if (idx <= numInserts) {
        // generate new transaction

        val tx = txGenerator.generate(account)
        //val future = writer ? SaveTransaction(tx.get)
        writer ! SaveTransaction(tx.get)
        //Await.ready(future, TIMEOUT)

        txs = txs ++ tx

      } else if (idx <= (numInserts+numUpdates)) {
        // update previously generated transaction
        val tx = txGenerator.update(txs(idx % numInserts))
        //val future = writer ? SaveTransaction(tx)
        writer ! SaveTransaction(tx)


      } else {
        //val future = writer ? DeleteTransaction(txs(idx % numInserts))
        writer ! DeleteTransaction(txs(idx % numInserts))

      }

      txCount += 1
      OPERATIONS +=1

      RandomGeneratorActor.synchronized {
        val durationSinceLastOutput = System.currentTimeMillis() - lastOutput;
        if (durationSinceLastOutput > reportInterval) {
          report(
            OPERATIONS,
            System.currentTimeMillis() - START_TIME)

          //OplogTailActor.fromBSONTimestamp(doc.get("ts").get.asInstanceOf[BSONTimestamp]));
          lastOutput = System.currentTimeMillis();
        }
      }
    }


    self ! Process(None)

  }

  /**
   * Prints progress.
   *
   * @param totalCount
   * @param duration
   */
  private def report(totalCount: Long, duration: Long) {
    val brate = totalCount.asInstanceOf[Double] / (duration / 1000.0)

    logger.info(s"throughput: ${longFmt.format(brate)} req/sec")

  }
}