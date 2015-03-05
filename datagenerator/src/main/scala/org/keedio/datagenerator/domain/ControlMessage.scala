package org.keedio.datagenerator.domain

import org.keedio.common.message.Message
import org.keedio.domain.{Transaction, Account}

case class SaveTransaction(tx:AnyRef) extends Message
case class DeleteTransaction(tx:AnyRef) extends Message
