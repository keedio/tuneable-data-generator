package org.keedio.datagenerator

import javax.sql.DataSource

import akka.actor.{ActorSystem, Props}
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.classic.db.DBAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.net.SyslogAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.db.DataSourceConnectionSource
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.{Appender, FileAppender, ConsoleAppender}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.apache.commons.lang3.Validate
import org.keedio.common.message.{Stop, Start}
import org.keedio.datagenerator.config.{DataGeneratorConfigAware, DataGeneratorConfig, SpringActorProducer}
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.context.annotation.AnnotationConfigApplicationContext

object Main extends App with DataGeneratorConfigAware {

  def configureLogger: Logger = {

    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[ch.qos.logback.classic.Logger]

    rootLogger.setLevel(Level.valueOf(keedioConfig.getString("default.logLevel")))

    val logEncoder = new PatternLayoutEncoder()
    logEncoder.setContext(ctx)
    logEncoder.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%-20logger{0}] %message%n")
    logEncoder.start()

    val logConsoleAppender = new ConsoleAppender()
    logConsoleAppender.setContext(ctx)
    logConsoleAppender.setName("STDOUT")
    logConsoleAppender.setEncoder(logEncoder.asInstanceOf[Encoder[Nothing]])
    logConsoleAppender.start()

    rootLogger
      .addAppender(logConsoleAppender.asInstanceOf[Appender[ILoggingEvent]])

    if (activeActor.equals("sysloggerActor")) {
      val syslogHost = keedioConfig.getString("syslog.host")
      val syslogFacility = keedioConfig.getString("syslog.facility")
      val syslogPort = keedioConfig.getInt("syslog.port")

      val appender = new SyslogAppender()
      appender.setPort(syslogPort)
      appender.setContext(ctx)
      appender.setName("SYSLOG")
      appender.setSyslogHost(syslogHost)
      appender.setFacility(syslogFacility)
      appender.setSuffixPattern("keedio.datagenerator: %date{yyyy-MM-dd HH:mm:ss.SSS} %level %logger{10} %msg%n")
      appender.start()

      val syslogLogger = LoggerFactory.getLogger("syslogLogger")
        .asInstanceOf[ch.qos.logback.classic.Logger]
      syslogLogger.setAdditive(false)
      syslogLogger.addAppender(appender)

    }
    else if (activeActor.equals("fileWriterActor")) {
      val encoder = new PatternLayoutEncoder()
      encoder.setContext(ctx)
      encoder.setPattern("%date{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%-20logger{0}] %message%n")
      encoder.start()

      val appender = new FileAppender()
      appender.setContext(ctx)
      appender.setName("FILE")
      appender.setFile(keedioConfig.getString("fileAppender.output"))
      appender.setEncoder(encoder.asInstanceOf[Encoder[Nothing]])
      appender.start()

      val fileLogger = LoggerFactory.getLogger("fileLogger")
        .asInstanceOf[ch.qos.logback.classic.Logger]
      fileLogger.setAdditive(false)
      fileLogger.addAppender(appender.asInstanceOf[Appender[ILoggingEvent]])
    }
    else if (activeActor.equals("dbWriterActor")) {

      val ds = new ComboPooledDataSource()
      ds.setDriverClass(keedioConfig.getString("dbWriter.driverClass"))
      ds.setJdbcUrl(keedioConfig.getString("dbWriter.jdbcUrl"))
      ds.setUser(keedioConfig.getString("dbWriter.user"))
      ds.setPassword(keedioConfig.getString("dbWriter.password"))

      val dataSourceConnectionSource = new DataSourceConnectionSource
      dataSourceConnectionSource.setDataSource(ds)
      dataSourceConnectionSource.start()

      val appender = new DBAppender
      appender.setContext(ctx)
      appender.setName("DBAPPENDER")
      appender.setConnectionSource(dataSourceConnectionSource)
      appender.start()

      val fileLogger = LoggerFactory.getLogger("dbLogger")
        .asInstanceOf[ch.qos.logback.classic.Logger]
      fileLogger.setAdditive(false)
      fileLogger.addAppender(appender.asInstanceOf[Appender[ILoggingEvent]])
    }
    else print("Actor not recognized. Not initializing specific logger")

    LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
  }

  val logger = configureLogger

  sys addShutdownHook {
    logger.warn("Shutting down ...")
    actor ! Stop()
    actorSystem.shutdown()
    actorSystem.awaitTermination()

    logger.warn("Done shutting down")
  }


  System.setProperty("spring.profiles.active", "app");

  val ctx = new AnnotationConfigApplicationContext(classOf[DataGeneratorConfig])

  val actorSystem = ctx.getBean(classOf[ActorSystem])

  val actor = actorSystem.actorOf(
    Props(classOf[SpringActorProducer], ctx, "randomGeneratorActor"), "randomGeneratorActor")

  actor ! Start()
}