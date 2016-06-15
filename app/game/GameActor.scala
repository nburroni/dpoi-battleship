package game

import akka.actor.{Actor, ActorRef}
import controllers.Messages._

/**
  * Created by nico on 08/06/16.
  */
class GameActor(playerOne: ActorRef, playerTwo: ActorRef) extends Actor {

  var players: Map[ActorRef, PlayerData] = Map (
    playerOne -> new PlayerData(turn = true),
    playerTwo -> new PlayerData(turn = false)
  )

  playerOne ! MatchGame(self)
  playerTwo ! MatchGame(self)

  playerOne ! YourTurn
  playerTwo ! OpponentTurn

  def switchTurn(currentPlayer: ActorRef, playerData: PlayerData, rival: ActorRef, rivalData: PlayerData) = {
    playerData.hasTurn = false
    rivalData.hasTurn = true
    players = Map (
      currentPlayer -> playerData,
      rival -> rivalData
    )
    currentPlayer ! OpponentTurn
    rival ! YourTurn
  }

  override def receive = {
    case Fire(x, y) =>
      val fireCoords: Coords = Coords(x, y)
      val currentPlayer: ActorRef = sender
      val playerData = players.getOrElse(currentPlayer, new PlayerData())
      val rival: ActorRef = otherPlayer(currentPlayer)
      val rivalData = otherPlayerData(currentPlayer)
      if (players.get(currentPlayer).fold(false)(_.hasTurn)) {
        val hit: Boolean = rivalData.shipsOption.fold(false) {
          case placements: Map[ShipPlacement, Boolean] =>
            placements.map {
              case (placement, sunk) =>
                placement.contains(fireCoords)
            }.fold(false)(_ || _)
        }
        if (hit) {
          currentPlayer ! HitShot(x, y)
          rival ! OpponentHit(x, y)
        } else {
          currentPlayer ! MissedShot(x, y)
          rival ! OpponentMissed(x, y)
        }
        rivalData.gridOption = rivalData.gridOption.map(fireCoords :: _)
        switchTurn(currentPlayer, playerData, rival, rivalData)
      }

    case PlacedShips(placements) =>
      players.get(sender).fold() { data =>
        data.shipsOption = Some(placements.map(_ -> false).toMap)
        data.gridOption = Some(List())
      }
      if (otherPlayerData(sender).shipsOption.isDefined) emit(GameReady)

  }

  def emit(msg: Message) = players foreach (_._1 ! msg)

  def otherPlayer(currentPlayer: ActorRef): ActorRef = (players - currentPlayer).head._1
  def otherPlayerData(currentPlayer: ActorRef): PlayerData = (players - currentPlayer).head._2

  case class PlayerData(turn: Boolean = false) {
    var hasTurn = turn
    var gridOption: Option[List[Coords]] = None
    var shipsOption: Option[Map[ShipPlacement, Boolean]] = None
  }

}