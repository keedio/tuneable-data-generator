package org.keedio.datagenerator

import java.io._
import java.util.UUID

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.compress.compressors.{CompressorException, CompressorStreamFactory}
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.lang3.RandomStringUtils._
import org.apache.commons.lang3.RandomUtils._
import org.apache.commons.lang3.StringUtils
import org.keedio.datagenerator.config.DataGeneratorConfigAware
import org.keedio.domain.{Account, AccountId, AccountTransaction}

import scala.collection.immutable.IndexedSeq
import scala.util.{Failure, Random, Success, Try}

case class GeneratorFactory() extends DataGeneratorConfigAware with LazyLogging {
  def getGenerator: DataGenerator = {
    if (StringUtils.isEmpty(keedioConfig.getString("inputFileGenerator.sourceFile")))
      DataAccountTransactionGenerator()
    else {
      val generator = FromInputFileGenerator(keedioConfig.getString("inputFileGenerator.sourceFile"))

      logger.info(s"Reading log messages from ${keedioConfig.getString("inputFileGenerator.sourceFile")}")

      generator
    }

  }
}

sealed trait DataGenerator {
  def generate(params: Option[AnyRef]): Option[AnyRef]

  def update(param: AnyRef): AnyRef

  def close(): Unit
}

case class FromInputFileGenerator(filename: String) extends DataGenerator with LazyLogging {
  var compressor: Option[BufferedReader] = None

  def wrapFileInputStream(): InputStream = {
    val fileInputStream = new BufferedInputStream(new FileInputStream(filename))

    try {
      new CompressorStreamFactory().createCompressorInputStream(fileInputStream)
    } catch {
      case e: CompressorException => fileInputStream
    }
  }

  def init(): Option[BufferedReader] = {
    if (compressor == None) {
      logger.info(s"(Re)initializing input stream for '$filename'")

      compressor = Some(new BufferedReader(
        new InputStreamReader(wrapFileInputStream())))
    }
    compressor
  }

  override def generate(params: Option[AnyRef]): Option[AnyRef] = {
    init()

    val line = compressor.get.readLine()

    if (line == null) {
      close()
      generate(params)
    } else {
      Some(line)
    }
  }

  override def update(param: AnyRef): AnyRef = ???

  override def close(): Unit = {
    IOUtils.closeQuietly(compressor.get)
    compressor = None
  }
}

case class DataAccountGenerator() extends DataGenerator {

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {
    val bank = "1234"
    val branch = random(4, false, true)
    val controlDigits = "09"
    val number = random(10, false, true)
    val accountId = AccountId(bank, branch, controlDigits, number)

    val userId = random(6, "123456789")
    val bankId = "1"
    val balance = random(6, "123456789")
    val currency = "001";
    val alias = random(15, true, false)

    Some(Account(userId, bankId, bankId, balance, alias, currency, accountId))
  }

  override def update(param: AnyRef): AnyRef = ???

  override def close(): Unit = {
  }
}

case class DataAccountTransactionGenerator() extends DataGenerator with LazyLogging {
  
  val cities: Array[(String, String, String)] = {
    var ois: ObjectInputStream = null
      try {
        ois = new ObjectInputStream(new FileInputStream(new File(System.getProperty("cities.inputfile"))))
        ois.readObject().asInstanceOf[Array[(String, String, String)]]
      } catch {
        case e: Exception => null
      } finally {
        IOUtils.closeQuietly(ois)
      }
  }
  
  def assignProbabilities(cities: Array[(String, String, String)]): List[((String, String, String), Double)] = {
    val cLength = cities.length
    
    val topIdxsSet: Set[Int] = Set(
      Random.nextInt(cLength),
      Random.nextInt(cLength),
      Random.nextInt(cLength),
      Random.nextInt(cLength),
      Random.nextInt(cLength)
    )
    
    if (topIdxsSet.size < 5){
      logger.error("topIxs size is not 5!")
      System.exit(1)
    }
    
    val topIdxs = topIdxsSet.toList
    
    val bottomIdxs = List(
      if (topIdxs(0) == 0) 1 else topIdxs(0)-1,
      if (topIdxs(1) == 0) 1 else topIdxs(1)-1,
      if (topIdxs(2) == 0) 1 else topIdxs(2)-1,
      if (topIdxs(3) == 0) 1 else topIdxs(3)-1,
      if (topIdxs(4) == 0) 1 else topIdxs(4)-1
    )
    
    val eqProb = 1.0/cLength
    
    val probMultiplier = 10
    
    val distributions = (for (ii <- 0 to (cLength-1)) yield {
      if (topIdxs.contains(ii)){
        logger.info(s"${cities(ii)} has probability ${eqProb*probMultiplier}")
        cities(ii) -> eqProb*probMultiplier
      } else {
        cities(ii) -> (1.0-eqProb*probMultiplier*5)/(cLength-5)
      }
    }).toList
    
    logger.info(s"${distributions.foldLeft[Double](0.0){_ + _._2}}")
    
    distributions.sortBy(pair => pair._2).reverse
  }
  
  val probabilities = Try(assignProbabilities(cities))
  
  val r = new Random()
  
  val currencies = Array("USD", "EUR", "GBP")
  val txType = Array("payment", "transfer")

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {
    val quantity = ("" + random(4, false, true)).toLong
    val relativeBalance = (random(5, false, true) + "." + random(3, false, true)).toDouble
    val valueDate = "" + System.currentTimeMillis()
    val description = random(20, true, false)
    val reference = random(16, false, true) + " " + random(8, false, true)
    val payer = random(15, true, false)
    val payee = random(15, false, true)
    val transactionType = txType(r.nextInt(txType.length))
    val srcGeoPosition = sample(probabilities)
    val destGeoPosition = sample(probabilities)
    val qty: Double = r.nextDouble()
    val currency = currencies(r.nextInt(currencies.length))
    
    val dest = Some(AccountTransaction(
      valueDate,
      valueDate, 
      description, 
      reference, 
      payer, 
      payee, 
      transactionType, 
      quantity, 
      relativeBalance, 
      params.asInstanceOf[Option[Account]],
      qty,
      currency,
      if (srcGeoPosition.isDefined) srcGeoPosition.get._1 else null,
      if (srcGeoPosition.isDefined) srcGeoPosition.get._2 else null,
      if (srcGeoPosition.isDefined) srcGeoPosition.get._3 else null,
      if (destGeoPosition.isDefined) destGeoPosition.get._1 else null,
      if (destGeoPosition.isDefined) destGeoPosition.get._2 else null,
      if (destGeoPosition.isDefined) destGeoPosition.get._3 else null))
    
    dest
  }

  final def sample[A <: Any](tryDist: Try[List[(A, Double)]]): Option[A] = {
    
    tryDist match {
      case Success(dist) => val p = scala.util.Random.nextDouble
        val it = dist.iterator
        var accum = 0.0
        while (it.hasNext) {
          val (item, itemProb) = it.next
          accum += itemProb
          if (accum >= p)
            return Some(item)  // return so that we don't have to search through the whole distribution
        }
        sys.error(f"this should never happen")  // needed so it will compile
      case Failure(e) => None
    }
    
    
  }

  override def update(itx: AnyRef): AnyRef = {
    val r = new Random()
    val idx = r.nextInt(3)

    val tx = itx.asInstanceOf[AccountTransaction]

    idx match {
      case 0 => tx.absQuantity = tx.quantity - Math.floor(0.1 * tx.quantity)
      case 1 => tx.description = random(20, true, false)
      case 2 => tx.reference = random(16, false, true) + " " + random(8, false, true)

    }

    tx
  }

  override def close(): Unit = {
  }
}