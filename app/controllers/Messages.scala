package controllers

import akka.actor.ActorRef

/**
  * Created by nico on 08/06/16.
  */
object Messages {

  case class InEvent(action: String, fire: Option[Fire] = None) {
    def toMessage: Message = {
      action match {
        case "search-game" => SearchGame
        case "fire" => fire.getOrElse(InvalidAction("invalid-fire"))
        //        case "fire-to" => FireTo(userKey, fire.get)
      }
    }
  }

  case class OutEvent(msg: String, fire: Option[Fire] = None)

  trait Message

  case class InvalidAction(msg: String) extends Message

  case class MatchPlayer(actor: ActorRef) extends Message

  case class MatchGame(gameActor: ActorRef) extends Message

  case object SearchGame extends Message

  case class Fire(x: Int, y: Int) extends Message

  case class MissedShot(x: Int, y: Int) extends Message

  case class OpponentMissed(x: Int, y: Int) extends Message

  case object YourTurn extends Message

  case object OpponentTurn extends Message

  //  case class FireTo(userKey: String, x: Int, y: Int) extends Message

}
