package org.keedio.actors

import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.annotation.{PreDestroy, PostConstruct}

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import com.codahale.metrics._
import com.codahale.metrics.ganglia.GangliaReporter
import com.google.common.util.concurrent.RateLimiter
import com.typesafe.scalalogging.slf4j.LazyLogging
import info.ganglia.gmetric4j.gmetric.GMetric
import info.ganglia.gmetric4j.gmetric.GMetric.UDPAddressingMode
import org.keedio.common.message._
import org.keedio.datagenerator.config.{DataGeneratorConfigAware, SpringActorProducer}
import org.keedio.datagenerator.domain.{DeleteTransaction, SaveTransaction}
import org.keedio.datagenerator.{DataGenerator, GeneratorFactory, DataAccountGenerator, DataAccountTransactionGenerator}
import org.keedio.domain.AccountTransaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import scala.collection.mutable
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.Random
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global

object RandomGeneratorActor {

}

@Component("randomGeneratorActor")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RandomGeneratorActor() extends Actor with LazyLogging with DataGeneratorConfigAware {

  var bytesProcessedHist: Histogram = _
  var operationsMeter: Meter = _

  val longFmt = new DecimalFormat("###,###")

  @Autowired
  var ctx: ApplicationContext = _

  var writer: ActorRef = _

  var rateLimiter: RateLimiter = _

  val TIMEOUT = Duration(30, SECONDS)

  implicit val timeout = Timeout(30 seconds)

  val metricRegistry = new MetricRegistry
  var jmxReporter: JmxReporter = _
  var gangliaReporter: GangliaReporter = _
  var consoleReporter: Slf4jReporter = _

  val txGenerator: DataGenerator = GeneratorFactory().getGenerator

  @PostConstruct
  def init(): Unit = {
    writer = context.actorOf(Props(classOf[SpringActorProducer], ctx, activeActor), activeActor)

    logger.info(s"Configuring rate limiter with $limitVal permits per second")
    rateLimiter = RateLimiter.create(limitVal)

    val prefix = keedioConfig.getString("ganglia.group.prefix")

    operationsMeter = metricRegistry.meter(MetricRegistry.name(prefix, "operations"))
    bytesProcessedHist = metricRegistry.histogram(MetricRegistry.name(prefix, "bytes_processed"))

    jmxReporter = JmxReporter.forRegistry(metricRegistry).build()

    val ganglia = new GMetric(
      keedioConfig.getString("ganglia.host"),
      keedioConfig.getInt("ganglia.port"),
      UDPAddressingMode.getModeForAddress(keedioConfig.getString("ganglia.host")), 1)

    gangliaReporter = GangliaReporter.forRegistry(metricRegistry)
      .build(ganglia)

    consoleReporter = Slf4jReporter.forRegistry(metricRegistry)
      .withLoggingLevel(Slf4jReporter.LoggingLevel.INFO)
      .outputTo(logger.underlying).build()
  }

  def receive = {
    case Start() =>
      logger.info("Received start message")

      jmxReporter.start()
      gangliaReporter.start(5, TimeUnit.SECONDS)
      consoleReporter.start(5, TimeUnit.SECONDS)
      //writer ! Start()
      doGenerateData()
    case Process(e) =>
      doGenerateData()
    case Stop() =>
      logger.warn("Received stop message")
      txGenerator.close()
      sender ! Ack

    case e:Any => logger.error(s"Unknown message of type: ${e.getClass}")
  }

  def doGenerateData(): Unit = {
    val r = Random

    //val accountGenerator = DataAccountGenerator()

    val numTxs = r.nextInt(numTxsPerAccount) + 1

    //val account = accountGenerator.generate()
    //writer ! SaveAccount(account.get)
    /*val accountFuture = writer ? SaveAccount(account.get)
    Await.ready(accountFuture, TIMEOUT)

    accountFuture onSuccess {
      case rc:Option[_] =>
    }
    */

    var txCount = 0

    var txs = mutable.MutableList[AnyRef]()

    val numDeletes: Int = Math.floor(numTxs * deleteRatio).asInstanceOf[Int]
    val numUpdates: Int = Math.floor(numTxs * updateRatio).asInstanceOf[Int]
    val numInserts: Int = numTxs - numUpdates - numDeletes

    //logger.debug(s"Account: ${account.get.ccc}: $numTxs events will be generated ($numInserts inserts, $numUpdates updates, $numDeletes deletes) ")

    for (idx <- 1 to numTxs) {
      rateLimiter.acquire()

      if (idx <= numInserts) {
        // generate new transaction

        val tx = txGenerator.generate(None)

        val future = writer ? SaveTransaction(tx.get)
        //writer ! SaveTransaction(tx.get)
        Await.ready(future, TIMEOUT).collect({
          case AckBytes(value:Long)=>
            bytesProcessedHist.update(value)
          case _ =>
        })

        txs = txs ++ tx

      } else if (idx <= (numInserts+numUpdates)) {
        // update previously generated transaction
        val tx = txGenerator.update(txs(idx % numInserts))
        val future = writer ? SaveTransaction(tx)
        //writer ! SaveTransaction(tx)
        Await.ready(future, TIMEOUT).collect({
          case AckBytes(value:Long)=>bytesProcessedHist.update(value)

          case _ =>
        })

      } else {
        val tx = txs(idx % numInserts)
        val future = writer ? DeleteTransaction(tx)
        //writer ! DeleteTransaction(txs(idx % numInserts))
        Await.ready(future, TIMEOUT).collect({
          case AckBytes(value:Long)=>bytesProcessedHist.update(value)
          case _ =>
        })
      }
      operationsMeter.mark()
    }

    self ! Process(None)
  }
}