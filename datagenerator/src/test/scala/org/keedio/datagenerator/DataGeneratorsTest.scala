package org.keedio.datagenerator

import org.apache.commons.lang3.StringUtils
import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.keedio.datagenerator.config.DataGeneratorConfigTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[DataGeneratorConfigTest]))
class DataGeneratorsTest  {

  //@Autowired
  // txRepo: TransactionRepository = _

  @Test
  def testAccountGenerator() {
    val accGen = RandomAccountGenerator()

    val account = accGen.generate().get

    assertNotNull(account)
    assertEquals("1234", account.accountId.bank)
    assertEquals(4, account.accountId.branch.length)
    assertEquals(2, account.accountId.controlDigits.length)
    assertEquals(10, account.accountId.number.length)

    assertNotNull(account.userId)
    assertNotNull(account.balance)
    assertEquals(15, account.alias.length)

  }

  @Test
  def testAccountTransactionGenerator() {
    val txGen = RandomAccountTransactionGenerator()

    val ntx = txGen.generate(None)

    assertEquals(None, ntx)

    val accGen = RandomAccountGenerator()
    val account = accGen.generate()
    
    val tx = txGen.generate(account).get

    assertNotNull(tx)
    assertTrue(tx.quantity < 0 )
    assertTrue(tx.relativeBalance > 0)
    assertTrue(StringUtils.isNotEmpty(tx.valueDate))
    assertTrue(StringUtils.isNotEmpty(tx.description))
    assertTrue(StringUtils.isNotEmpty(tx.reference))
    assertTrue(StringUtils.isNotEmpty(tx.payee))
    assertTrue(StringUtils.isNotEmpty(tx.payer))
    assertNotNull(tx.txType)
  }
}