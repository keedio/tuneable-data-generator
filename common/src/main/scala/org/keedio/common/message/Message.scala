package org.keedio.common.message

/**
 * Created by luca on 25/11/14.
 */
trait Message

case class Process(e:Option[_]) extends Message
case object Ack extends Message
case class Start() extends Message
case class Stop() extends Message
case class AckBytes(value: Long) extends Message