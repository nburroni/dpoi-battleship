package game

import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable}
import controllers.{FireMap, PlayerActor}
import controllers.Messages._

import scala.concurrent.duration._
import scala.util.Random

/**
  * Created by nico on 08/06/16.
  */
class GameActor(playerOne: PlayerActor, playerTwo: PlayerActor) extends Actor {

  var players: Map[ActorRef, PlayerData] = Map(
    playerOne.self -> new PlayerData(turn = true, id = playerOne.id),
    playerTwo.self -> new PlayerData(turn = false, id = playerTwo.id)
  )

  playerOne.self ! MatchGame(self, playerTwo.id)
  playerTwo.self ! MatchGame(self, playerOne.id)

  playerOne.self ! YourTurn
  playerTwo.self ! OpponentTurn

  def switchTurn(currentPlayer: ActorRef, playerData: PlayerData, rival: ActorRef, rivalData: PlayerData) = {

    playerData.hasTurn = false
    rivalData.hasTurn = true
    players = Map(
      currentPlayer -> playerData,
      rival -> rivalData
    )
    currentPlayer ! OpponentTurn
    rival ! YourTurn
  }

  override def receive = {
    case GameTimeout =>
      val rival: ActorRef = otherPlayer(sender)
      sender ! TimeoutLost
      rival ! TimeoutWon

    case s: SetReconnect =>
      if (players(s.player).shipsOption.isDefined && otherPlayerData(s.player).shipsOption.isDefined) {
        GameManager setReconnect(players(s.player).id, s.player, self)
      }

    case AutoFire =>
      def getRandomCoord = new Random().nextInt(10)

      var result = fire(getRandomCoord, getRandomCoord)
      while (result) {
        result = fire(getRandomCoord, getRandomCoord)
      }



    case Fire(x, y) =>
      sender ! CancelShotTimeout
      fire(x, y)

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
    case m: MatchData =>
      val playerData = players.getOrElse(sender, PlayerData())
      val rivalData = otherPlayerData(sender)
      GameManager saveData(playerData.id, m.stats.head, rivalData.id)
      GameManager saveData(rivalData.id, m.stats(1), playerData.id)
      context.stop(self)
  }

  private def fire(x: Int, y: Int) = {
    val fireCoords: Coords = Coords(x, y)
    val currentPlayer: ActorRef = sender
    val playerData = players.getOrElse(currentPlayer, PlayerData())
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
          if (sunken) {
            val allSunken = rivalData.shipsOption.fold(false)(_.forall(_._2.isSunk))
            if (allSunken) {
              currentPlayer ! YouWon(x, y)
              rival ! YouLost(x, y)
              rival ! CancelShotTimeout
              sender ! CancelShotTimeout
            } else {
              rival ! SetShotTimeout

              currentPlayer ! SunkShip(x, y)
              rival ! OpponentSunkShip(x, y)
            }
          } else {
            rival ! SetShotTimeout

            currentPlayer ! HitShot(x, y)
            rival ! OpponentHit(x, y)
          }
        } else {
          rival ! SetShotTimeout
          currentPlayer ! MissedShot(x, y)
          rival ! OpponentMissed(x, y)
        }
        rivalData.gridOption = rivalData.gridOption.map(fireCoords :: _)
        switchTurn(currentPlayer, playerData, rival, rivalData)
      }
      alreadyShot
    } else true
  }

  def emit(msg: Message) = players foreach (_._1 ! msg)

  def otherPlayer(currentPlayer: ActorRef): ActorRef = (players - currentPlayer).head._1

  def otherPlayerData(currentPlayer: ActorRef): PlayerData = (players - currentPlayer).head._2

}

case class PlayerData(turn: Boolean = false, id: String = "-1") {
  var hasTurn = turn
  var gridOption: Option[List[Coords]] = None
  var shipsOption: Option[Map[ShipPlacement, Sunk]] = None
  var mySunkenShips: Int = 0
}

case class ReconnectData(turn: Boolean, oppFires: List[FireMap], ships: List[ShipPlacement], myFires: List[FireMap], oppId: String)