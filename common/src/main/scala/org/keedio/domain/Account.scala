package org.keedio.domain

/**
 * Created by Luca Rosellini <lrosellini@keedio.com> on 11/2/15.
 */
case class Account(userId: String, bankId: String, systemBank: String, balance: String, alias: String, currency: String, accountId: AccountId) {
  def ccc = s"${accountId.bank}-${accountId.branch}-${accountId.controlDigits}-${accountId.number}"
}

case class AccountId(bank: String, branch: String, controlDigits: String, number: String)