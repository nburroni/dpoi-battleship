package game

import akka.actor.{Actor, ActorRef}
import controllers.Messages._

/**
  * Created by nico on 08/06/16.
  */
class GameActor(playerOne: ActorRef, playerTwo: ActorRef) extends Actor {

  var players: Map[ActorRef, Boolean] = Map(
    playerOne -> true,
    playerTwo -> false
  )

  playerOne ! MatchGame(self)
  playerTwo ! MatchGame(self)

  playerOne ! YourTurn
  playerTwo ! OpponentTurn

  def switchTurn(currentPlayer: ActorRef, rival: ActorRef) = {
    players = Map(
      currentPlayer -> false,
      rival -> true
    )
    currentPlayer ! OpponentTurn
    rival ! YourTurn
  }

  override def receive = {
    case Fire(x, y) =>
      val currentPlayer: ActorRef = sender
      val rival: ActorRef = (players - currentPlayer).head._1
      if (players.getOrElse(currentPlayer, false)) {
        // TODO check if missed or hit
        currentPlayer ! MissedShot(x, y)
        rival ! OpponentMissed(x, y)
        switchTurn(currentPlayer, rival)
      }
  }
}