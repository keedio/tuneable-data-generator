package org.keedio.config

import com.typesafe.config.ConfigFactory

/**
 * Created by luca on 26/11/14.
 */
trait SparkConfig extends ConfigAware {
  val sparkMaster = keedioConfig.getString("spark.master")
  val appName = keedioConfig.getString("spark.appname")
  val jars: Array[String] = keedioConfig.getString("spark.jars").split(",")
}
