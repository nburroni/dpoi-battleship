package game

import akka.actor.{Actor, ActorRef}
import controllers.{PlayerActor, FireMap}
import controllers.Messages._

/**
  * Created by nico on 08/06/16.
  */
class GameActor(playerOne: PlayerActor, playerTwo: PlayerActor) extends Actor {

  var players: Map[ActorRef, PlayerData] = Map (
    playerOne.self -> new PlayerData(turn = true),
    playerTwo.self -> new PlayerData(turn = false)
  )

  playerOne.self ! MatchGame(self, playerTwo.id)
  playerTwo.self ! MatchGame(self, playerOne.id)

  playerOne.self ! YourTurn
  playerTwo.self ! OpponentTurn

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
        val alreadyShot = rivalData.gridOption.fold(false)(_.contains(fireCoords))

        if (!alreadyShot) {
          var sunken = false
          val hit: Boolean = rivalData.shipsOption.fold(false) {
            case placements: Map[ShipPlacement, Sunk] =>
              placements.map {
                case (placement, sunk) =>
                  if (!sunk.isSunk && placement.contains(fireCoords)) {
                    placement.alterLives -= 1
                    if (placement.alterLives == 0) {
                      sunk.isSunk = true
                      sunken = true
                      playerData.mySunkenShips += 1
                    }
                    true
                  } else {
                    false
                  }
              }.fold(false)(_ || _)
          }
          if (hit) {
            if(sunken){
              val allSunken = rivalData.shipsOption.fold(false)(_.forall(_._2.isSunk))
              if (allSunken){
                currentPlayer ! YouWon(x,y)
                rival ! YouLost(x,y)
              }else{
                currentPlayer ! SunkShip(x, y)
                rival ! OpponentSunkShip(x, y)
              }
            }else{
              currentPlayer ! HitShot(x, y)
              rival ! OpponentHit(x, y)
            }
          } else {
            currentPlayer ! MissedShot(x, y)
            rival ! OpponentMissed(x, y)
          }
          rivalData.gridOption = rivalData.gridOption.map(fireCoords :: _)
          switchTurn(currentPlayer, playerData, rival, rivalData)
        }
      }

    case PlacedShips(placements) =>
      players.get(sender).fold() { data =>
        data.shipsOption = Some(placements.map(_ -> Sunk(false)).toMap)
        data.gridOption = Some(List())
      }
      if (otherPlayerData(sender).shipsOption.isDefined) emit(GameReady)

    case Reconnect(newP, prevP, prevId) =>
      players = players + (newP -> players(prevP))
      players = players - prevP
      val data = players(newP)
      val rivalData = otherPlayerData(newP)
      GameManager successfulReconnection prevId
      newP ! Reconnected(self, data, rivalData)
  }

  def emit(msg: Message) = players foreach (_._1 ! msg)

  def otherPlayer(currentPlayer: ActorRef): ActorRef = (players - currentPlayer).head._1
  def otherPlayerData(currentPlayer: ActorRef): PlayerData = (players - currentPlayer).head._2

}
case class PlayerData(turn: Boolean = false) {
  var hasTurn = turn
  var gridOption: Option[List[Coords]] = None
  var shipsOption: Option[Map[ShipPlacement, Sunk]] = None
  var mySunkenShips: Int = 0
}

case class ReconnectData(turn: Boolean, oppFires: List[FireMap], ships: List[ShipPlacement], myFires: List[FireMap])