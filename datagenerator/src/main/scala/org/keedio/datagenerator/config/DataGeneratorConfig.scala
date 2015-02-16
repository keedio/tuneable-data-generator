package org.keedio.datagenerator.config

import akka.actor.ActorSystem
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration, Import, ImportResource}

@Configuration
@Import(Array(classOf[DataGeneratorConfigApp], classOf[DataGeneratorConfigTest]))
@ComponentScan(Array("org.keedio.actors"))
//@ImportResource(Array("classpath*:META-INF/spring/datageneratorPersistence.xml"))
class DataGeneratorConfig {
  /*
  @Autowired
  var txRepo: TransactionRepository = _
  
  @Autowired
  var ctx : ApplicationContext = _
  */
  /**
   * Actor system singleton for this application.
   */
  @Bean(name=Array("actorSystem"))
  def actorSystem = {
    ActorSystem("AkkaScalaSpring")
  }
}