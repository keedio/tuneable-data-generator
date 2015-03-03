package org.keedio.domain

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 11/2/15.
 */
trait Transaction {

}

case class AccountTransaction(var valueDate: String,
                              var operationDate: String,
                              var description: String,
                              var reference: String,
                              var payer: String,
                              var payee: String,
                              var txType: String,
                              var quantity: Double,
                              var relativeBalance: Double,
                              var owner: Option[Account],
                              var absQuantity: Double = 0) extends Transaction