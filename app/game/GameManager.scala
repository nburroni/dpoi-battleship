package game

/**
  * Created by nico on 28/05/16.
  */

import akka.actor.{ActorSystem, Props, ActorRef}
import play.api._

import scala.collection.immutable.Queue


object GameManager extends GlobalSettings {

  private var pendingPlayers = Queue[ActorRef]()

  def addPlayer(player: ActorRef) = {
    getFirstPendingPlayer match {
      case Some(rival) =>
        if (rival == player) {
          pendingPlayers = pendingPlayers enqueue player
        } else {
          val props: Props = Props(new GameActor(rival, player))
          ActorSystem("mySystem").actorOf(props, "gameActor")
        }
      case None =>
        pendingPlayers = pendingPlayers enqueue player
    }
  }

  def getFirstPendingPlayer: Option[ActorRef] = {
    if (pendingPlayers.isEmpty) None
    else {
      pendingPlayers.dequeue match {
        case (result, queue) =>
          pendingPlayers = queue
          Some(result)
      }
    }
  }

}