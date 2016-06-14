package util

import akka.actor.{ActorSystem, Actor}
import scala.concurrent.duration._

/**
  * Created by nico on 14/06/16.
  */
case object Timer extends Actor {

  val scheduler = ActorSystem("system").scheduler

  override def receive = {
    case Schedule(time) =>
//      scheduler.scheduleOnce(time)
    case Start =>
    case Stop =>
    case Pause =>
  }

  case class Schedule(time: FiniteDuration)(f: => Unit)
  case object Start
  case object Stop
  case object Pause
}
