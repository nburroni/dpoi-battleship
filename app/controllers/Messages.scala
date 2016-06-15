package controllers

import akka.actor.ActorRef

/**
  * Created by nico on 08/06/16.
  */
object Messages {

  case class ActionIn(action: String, fire: Option[Fire] = None, ships: Option[List[ShipPlacement]]) {
    def toMessage: Message = {
      action match {
        case "search-game" => SearchGame
        case "fire" => fire.getOrElse(InvalidAction("invalid-fire"))
        case "placed-ships" => PlacedShips(ships.getOrElse(List()))
      }
    }
  }

  case class ActionOut(msg: String, fire: Option[Fire] = None)

  trait Message

  case class InvalidAction(msg: String) extends Message

  case class MatchPlayer(actor: ActorRef) extends Message

  case class MatchGame(gameActor: ActorRef) extends Message

  case object SearchGame extends Message

  case class Fire(x: Int, y: Int) extends Message

  case class MissedShot(x: Int, y: Int) extends Message
  case class HitShot(x: Int, y: Int) extends Message
  case class SunkShip(x: Int, y: Int) extends Message

  case class OpponentMissed(x: Int, y: Int) extends Message
  case class OpponentHit(x: Int, y: Int) extends Message
  case class OpponentSunkShip(x: Int, y: Int) extends Message

  case class PlacedShips(placements: List[ShipPlacement]) extends Message

  case object GameReady extends Message

  case class ShipPlacement(start: Coords, end: Coords, lives: Int) {
    var alterLives = lives
    def contains(coords: Coords) = {
      (start.x <= coords.x && coords.x <= end.x) && (start.y <= coords.y && coords.y <= end.y)
    }
  }

  case class Sunk(sunk: Boolean){
    var isSunk = sunk
  }

  case class Coords(x: Int, y: Int)

  case object YourTurn extends Message

  case object OpponentTurn extends Message

  case class YouWon(x: Int, y: Int) extends Message

  case class YouLost(x: Int, y: Int) extends Message
}
