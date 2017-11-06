package org.keedio.datagenerator

import javax.sql.DataSource

import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.classic.db.DBAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.net.SyslogAppender
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.db.DataSourceConnectionSource
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.rolling._
import ch.qos.logback.core.{Appender, ConsoleAppender, FileAppender}
import com.mchange.v2.c3p0.ComboPooledDataSource
import org.apache.commons.lang3.Validate
import org.keedio.common.message.{Start, Stop}
import org.keedio.datagenerator.config.{DataGeneratorConfig, DataGeneratorConfigAware, SpringActorProducer}
import org.omg.CORBA.TIMEOUT
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import akka.pattern.ask
import ch.qos.logback.core.net.SyslogOutputStream
import com.papertrailapp.logback.Syslog4jAppender
import org.productivity.java.syslog4j.SyslogConstants
import org.productivity.java.syslog4j.impl.net.tcp.TCPNetSyslogConfig

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with DataGeneratorConfigAware {

  def configureLogger: Logger = {

    val sharedPattern = "%msg%n"


    val ctx = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
      .asInstanceOf[ch.qos.logback.classic.Logger]

    rootLogger.detachAndStopAllAppenders()
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

      val appender = new Syslog4jAppender[ILoggingEvent]
      
      val config = new TCPNetSyslogConfig(8, syslogHost, syslogPort)
      config.setSoLinger(true)
      config.setSoLingerSeconds(12345)
      config.setKeepAlive(true)
      config.setPersistentConnection(true)
      config.setMaxMessageLength(128000)
      config.setIdent("tuneable-data-generator")
      
      //config.setSendLocalName(true)
      //config.setCharSet("UTF-8")
      appender.setSyslogConfig(config)
      appender.setContext(ctx)
      appender.start()
      
      val syslogLogger = LoggerFactory.getLogger("syslogLogger")
        .asInstanceOf[ch.qos.logback.classic.Logger]
      
      syslogLogger.setAdditive(false)
      syslogLogger.addAppender(appender)
      syslogLogger.info(s"syslog appender configured: ${appender.isStarted}")
      syslogLogger.error(s"syslog appender configured: ${appender.isStarted}")
      println(s"syslog appender configured: ${appender.isStarted}")

    }
    else if (activeActor.equals("fileWriterActor") || activeActor.equals("jsonFileWriterActor")) {
      val encoder = new PatternLayoutEncoder()
      encoder.setContext(ctx)
      encoder.setPattern(sharedPattern)
      encoder.start()
      
      val appender = new RollingFileAppender
      appender.setPrudent(true)

      val triggeringPolicy = new SizeBasedTriggeringPolicy
      triggeringPolicy.setMaxFileSize(rollingSize)
      triggeringPolicy.setContext(ctx)

      val rollingPolicy = new TimeBasedRollingPolicy
      rollingPolicy.setParent(appender)
      //rollingPolicy.setMaxIndex(1)
      rollingPolicy.setMaxHistory(1000)
      rollingPolicy.setFileNamePattern(s"${keedioConfig.getString("fileAppender.output")}.%d{yyyy-MM-dd_HH-mm}.log")
      rollingPolicy.setContext(ctx)

      appender.setContext(ctx)
      appender.setName("FILE")
      //appender.setFile(keedioConfig.getString("fileAppender.output"))
      appender.setEncoder(encoder.asInstanceOf[Encoder[Nothing]])
      appender.setRollingPolicy(rollingPolicy)
      appender.setTriggeringPolicy(triggeringPolicy)

      triggeringPolicy.start()
      rollingPolicy.start()
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

  implicit val timeout = Timeout(30 seconds)

  sys addShutdownHook {
    logger.warn("Shutting down ...")
    
    val future = actor ? Stop()

    val TIMEOUT = Duration(10, SECONDS)
    Await.ready(future, TIMEOUT)

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

class TGSyslogOutputStream(host: String, port: Int) extends SyslogOutputStream(host, port){
  
}