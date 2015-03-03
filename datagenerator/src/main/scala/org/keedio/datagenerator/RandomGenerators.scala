package org.keedio.datagenerator

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.apache.commons.lang3.RandomStringUtils
import org.keedio.domain.{AccountTransaction, AccountId, Account}
import scala.collection.JavaConversions._
import scala.util.Random

sealed trait RandomGenerator[T, S] {
  def generate(params: Option[S]): Option[T]

  def update(param: T): T
}

case class RandomAccountGenerator() extends RandomGenerator[Account, Nothing] {

  override def generate(params: Option[Nothing] = None): Option[Account] = {
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

  override def update(param: Account): Account = ???
}

case class RandomAccountTransactionGenerator() extends RandomGenerator[AccountTransaction, Account] with LazyLogging {
  val r = new Random()

  override def generate(params: Option[Account] = None): Option[AccountTransaction] = {
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
      valueDate, description, reference, payer, payee, transactionType, quantity, relativeBalance, params))



  }

  override def update(tx: AccountTransaction): AccountTransaction = {
    val r = new Random()
    val idx = r.nextInt(3)

    idx match {
      case 0 => tx.absQuantity =  tx.quantity - Math.floor(0.1 * tx.quantity)
      case 1 => tx.description = RandomStringUtils.random(20, true, false)
      case 2 => tx.reference = RandomStringUtils.random(16, false, true) + " " + RandomStringUtils.random(8, false, true)

    }

    tx
  }
}