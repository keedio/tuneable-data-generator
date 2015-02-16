package org.keedio.datagenerator

import akka.actor.{ActorSystem, Props}
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.keedio.common.message.{Stop, Start}
import org.keedio.datagenerator.config.{DataGeneratorConfig, SpringActorProducer}
import org.springframework.context.annotation.AnnotationConfigApplicationContext

object Main extends App with LazyLogging {
  
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