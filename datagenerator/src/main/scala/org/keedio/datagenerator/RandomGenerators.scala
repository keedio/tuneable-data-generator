package org.keedio.datagenerator

import java.io._

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.compress.compressors.{CompressorException, CompressorStreamFactory}
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.lang3.RandomStringUtils
import org.keedio.datagenerator.config.DataGeneratorConfigAware
import org.keedio.domain.{AccountTransaction, AccountId, Account}
import scala.collection.JavaConversions._
import scala.util.Random

case class GeneratorFactory() extends DataGeneratorConfigAware with LazyLogging{
  def getGenerator: DataGenerator = {
    if (keedioConfig.getString("generator.type").equals("DataAccountTransactionGenerator"))
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

case class FromInputFileGenerator(filename: String) extends DataGenerator{
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

case class DataAccountGenerator() extends DataGenerator {

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {
    val bank = "1234"
    val branch = RandomStringUtils.random(4, false, true)
    val controlDigits = "09"
    val number = RandomStringUtils.random(10, false, true)
    val accountId = AccountId(bank, branch, controlDigits, number)

    val userId = RandomStringUtils.random(6, "123456789")
    val bankId = "1"
    val balance = RandomStringUtils.random(6, "123456789")
    val currency = "001";
    val alias = RandomStringUtils.random(15, true, false)

    Some(Account(userId, bankId, bankId, balance, alias, currency, accountId))
  }

  override def update(param: AnyRef): AnyRef = ???

  override def close(): Unit = {
  }
}

case class DataAccountTransactionGenerator() extends DataGenerator with LazyLogging {
  val r = new Random()

  override def generate(params: Option[AnyRef] = None): Option[AnyRef] = {
    val quantity = ("-" + RandomStringUtils.random(4, false, true)).toLong
    val relativeBalance = (RandomStringUtils.random(5, false, true) + "." + RandomStringUtils.random(3, false, true)).toDouble
    val valueDate = "" + System.currentTimeMillis()
    val description = RandomStringUtils.random(20, true, false)
    val reference = RandomStringUtils.random(16, false, true) + " " + RandomStringUtils.random(8, false, true)
    val payer = RandomStringUtils.random(15, true, false)
    val payee = RandomStringUtils.random(15, false, true)
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
      case 1 => tx.description = RandomStringUtils.random(20, true, false)
      case 2 => tx.reference = RandomStringUtils.random(16, false, true) + " " + RandomStringUtils.random(8, false, true)

    }

    tx
  }

  override def close(): Unit = {
  }
}