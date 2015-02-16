package org.keedio.datagenerator.config

import org.keedio.config.ConfigAware

/**
 * Created by luca on 26/11/14.
 */
trait DataGeneratorConfigAware extends ConfigAware {
  val numTxsPerAccount = keedioConfig.getInt("num_txs_per_generated_account")
  val updateRatio = keedioConfig.getDouble("update_ratio")
  val deleteRatio = keedioConfig.getDouble("delete_ratio")
  val limitVal = keedioConfig.getInt("rate.limiter")
  val activeActor = keedioConfig.getString("active.actor")
}
