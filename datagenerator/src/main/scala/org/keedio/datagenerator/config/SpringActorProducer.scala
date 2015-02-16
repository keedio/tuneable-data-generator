package org.keedio.datagenerator.config

import akka.actor.{Actor, IndirectActorProducer}
import org.springframework.context.{ApplicationContext => FCA}

class SpringActorProducer(ctx: FCA, actorBeanName: String) extends IndirectActorProducer {
  override def produce: Actor = ctx.getBean(actorBeanName, classOf[Actor])
  
  override def actorClass: Class[_ <: Actor] =
    ctx.getType(actorBeanName).asInstanceOf[Class[_ <: Actor]]
}