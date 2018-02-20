package controllers

import akka.actor.ActorRef
import game.{ReconnectData, PlayerData}

/**
  * Created by nico on 08/06/16.
  */
object Messages {

  case class ActionIn(action: String, fire: Option[Fire] = None, ships: Option[List[ShipPlacement]] = None, matchData: Option[MatchData] = None) {
    def toMessage: Message = {
      action match {
        case "search-game" => SearchGame
        case "fire" => fire.getOrElse(InvalidAction("invalid-fire"))
        case "placed-ships" => PlacedShips(ships.getOrElse(List()))
        case "save-player" => SavePlayer
        case "reconnect" => TryReconnect
        case "save-data" => matchData.getOrElse(InvalidAction("invalid-data"))
        case "stats" => GetStats
      }
    }
  }

  case class Reconnect(update: ActorRef, prev: ActorRef, prevId: String) extends Message

  case object SavePlayer extends Message

  case object TryReconnect extends Message

  case class ActionOut(msg: String, fire: Option[Fire] = None, data: Option[ReconnectData] = None, stats: Option[UserData] = None, oppId: Option[String] = None)

//  case class ReconnectData(msg: String, hasTurn: Boolean, gridOption: List[Coords], shipsOption: Map[ShipPlacement, Sunk]) extends Message

  trait Message

  case class InvalidAction(msg: String) extends Message

  case class MatchPlayer(actor: ActorRef) extends Message

  case class MatchGame(gameActor: ActorRef, id: String) extends Message

  case class Reconnected(gameActor: ActorRef, data: PlayerData, rivalData: PlayerData) extends Message

  case object SearchGame extends Message

  case object NotReconnected extends Message

  case class SetReconnect(player: ActorRef) extends Message

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

  case class MatchData(won: Boolean, hits: Int, misses: Int, time: Long) extends Message

  case class Sunk(sunk: Boolean){
    var isSunk = sunk
  }

  case class Coords(x: Int, y: Int)

  case object YourTurn extends Message

  case object OpponentTurn extends Message

  case class YouWon(x: Int, y: Int) extends Message

  case class YouLost(x: Int, y: Int) extends Message

  case object TimeoutLost extends Message

  case object TimeoutWon extends Message

  case class UserData(wins: Int, losses: Int, hits: Int, misses: Int) extends Message

  case object GetStats extends Message

  case object GameTimeout extends Message

  case object SetTimeout extends Message

  case object CancelTimeout extends Message
}
