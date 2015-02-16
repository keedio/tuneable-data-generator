package org.keedio.datagenerator.domain

import org.keedio.common.message.Message
import org.keedio.domain.{Transaction, Account}

case class SaveAccount(acc:Account) extends Message
case class SaveTransaction(tx:Transaction) extends Message
case class DeleteTransaction(tx:Transaction) extends Message
