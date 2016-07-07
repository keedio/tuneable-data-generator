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

import scala.util.Random

case class GeneratorFactory() extends DataGeneratorConfigAware with LazyLogging{
  def getGenerator: DataGenerator = {
    if (StringUtils.isEmpty(keedioConfig.getString("inputFileGenerator.sourceFile")))
      DataAccountTransactionGenerator()
    else{
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

case class FromInputFileGenerator(filename: String) extends DataGenerator with LazyLogging{
  var compressor: Option[BufferedReader] = None

  def wrapFileInputStream(): InputStream ={
    val fileInputStream = new BufferedInputStream(new FileInputStream(filename))

    try {
      new CompressorStreamFactory().createCompressorInputStream(fileInputStream)
    } catch {
      case e:CompressorException => fileInputStream
    }
  }

  def init(): Option[BufferedReader] = {
    if (compressor == None){
      logger.info(s"(Re)initializing input stream for '$filename'")

      compressor = Some(new BufferedReader(
          new InputStreamReader(wrapFileInputStream())))
    }
    compressor
  }

  override def generate(params: Option[AnyRef]): Option[AnyRef] = {
    init()

    val line = compressor.get.readLine()

    if (line == null){
      close()
      generate(params)
    } else{
      Some(line)
    }
  }

  override def update(param: AnyRef): AnyRef = ???

  override def close(): Unit = {
    IOUtils.closeQuietly(compressor.get)
    compressor = None
  }
}

case class EvoAccountMovementGenerator() extends DataGenerator with LazyLogging {
  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization._
  implicit val formats = Serialization.formats(NoTypeHints)

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {

    val m = Map("operationCardStatusIdDescription"-> random(10, true, false),
      "loadedCurrencyId"-> (if (nextInt(0,2) == 0) "EUR" else "USD"),
      "isInternationalOperation"-> (if (nextInt(0,2) == 0) true else false),
      "cardSequential"-> nextInt(0,100),
      "operationCountry"-> random(4, true, false),
      "uuid"-> UUID.randomUUID().toString,
      "type" -> "cardMovement",
      "categoryDescription"-> random(10, true, false),
      "associatedAccountId"-> nextLong(10000, 100000001),
      "associatedCardType"-> nextInt(0,5).toString,
      "operationLocation"-> random(3, false, true),
      "operationExchangeRate"-> 0,
      "isInstallmentPurchase"-> (if (nextInt(0,2) == 0) true else false),
      "associatedAccountType"-> random(12, false, true),
      "commerceId"-> random(8, false, true),
      "timestamp"-> "2016-06-21 12:15:47.572",
      "operationCardStatusId"-> (if (nextInt(0,2) == 0) "000" else "001"),
      "merchantCode"-> random(4, false, true),
      "entityCode"-> random(4, false, true),
      "edited"-> (if (nextInt(0,2) == 0) true else false),
      "operationCardDescription"-> random(20, true, false),
      "operationCurrencyId"-> (if (nextInt(0,2) == 0) "EUR" else "USD"),
      "cancelledOperation"-> (if (nextInt(0,2) == 0) true else false),
      "operationCardType"-> nextInt(0,10).toString,
      "movementTypeId"-> (if (nextInt(0,2) == 0) "TJ" else "TJ-B"),
      "operationCardSign"-> (if (nextInt(0,2) == 0) true else false),
      "loadedAmount"-> nextDouble(0.0, 100000.0),
      "associatedCardId"-> random(16, false, true),
      "categoryBusinessDescription"-> random(10, true, false),
      "operationCardDate"-> "2016-06-21 13:15:47.572",
      "cardId"-> random(16, false, true),
      "operationAmount"-> nextDouble(0.0, 100000.0),
      "terminalCode"-> random(5, false, true),
      "categoryId"-> nextInt(0,51),
      "isCreditOperation" -> (if (nextInt(0,2) == 0) true else false)
    )

    val output = write(m)

    logger.info(s"Generating message: $output")

    Some(output)
  }

  override def update(param: AnyRef): AnyRef = {
    param
  }

  override def close(): Unit = {
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
  val r = new Random()

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {
    val quantity = ("-" + random(4, false, true)).toLong
    val relativeBalance = (random(5, false, true) + "." + random(3, false, true)).toDouble
    val valueDate = "" + System.currentTimeMillis()
    val description = random(20, true, false)
    val reference = random(16, false, true) + " " + random(8, false, true)
    val payer = random(15, true, false)
    val payee = random(15, false, true)
    val transactionType = "" + r.nextInt(6)
    Some(AccountTransaction(
      valueDate,
      valueDate, description, reference, payer, payee, transactionType, quantity, relativeBalance, params.asInstanceOf[Option[Account]]))



  }

  override def update(itx: AnyRef): AnyRef = {
    val r = new Random()
    val idx = r.nextInt(3)

    val tx = itx.asInstanceOf[AccountTransaction]

    idx match {
      case 0 => tx.absQuantity =  tx.quantity - Math.floor(0.1 * tx.quantity)
      case 1 => tx.description = random(20, true, false)
      case 2 => tx.reference = random(16, false, true) + " " + random(8, false, true)

    }

    tx
  }

  override def close(): Unit = {
  }
}