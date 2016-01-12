package org.keedio.datagenerator.config

import com.typesafe.config.ConfigException.Missing
import org.apache.commons.lang3.StringUtils
import org.keedio.config.ConfigAware

/**
 * Created by luca on 26/11/14.
 */
trait DataGeneratorConfigAware extends ConfigAware {
  val numTxsPerAccount = keedioConfig.getInt("num_txs_per_generated_account")
  val updateRatio = if (StringUtils.isEmpty(keedioConfig.getString("inputFileGenerator.sourceFile"))) keedioConfig.getDouble("update_ratio") else 0.0
  val deleteRatio =  if (StringUtils.isEmpty(keedioConfig.getString("inputFileGenerator.sourceFile"))) keedioConfig.getDouble("delete_ratio") else 0.0
  val limitVal = keedioConfig.getInt("rate.limiter")
  val activeActor = keedioConfig.getString("active.actor")
  val rollingSize = try { keedioConfig.getString("fileAppender.rollingSize") } catch { case e:Missing => "1MB" }
}
