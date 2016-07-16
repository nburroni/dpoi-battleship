package game

/**
  * Created by nico on 28/05/16.
  */

import akka.actor.{ActorSystem, Props, ActorRef}
import controllers.Messages.{NotReconnected, Reconnect}
import play.api._

import scala.collection.immutable.Queue


object GameManager extends GlobalSettings {
  def successfulReconnection(prevId: String) = {
    reconnectPlayers -= prevId
  }

  private var reconnectPlayers: Map[String, (ActorRef, ActorRef)] = Map()
  private var pendingPlayers = Queue[ActorRef]()


  def setReconnect(_id: String, player: ActorRef, game: ActorRef) = {
    if (!reconnectPlayers.contains(_id)) {
      reconnectPlayers += (_id -> (player, game))
    }
  }

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

  def tryReconnect(_id: String, player: ActorRef) = {
    var rec = false
    reconnectPlayers.get(_id).foreach { tuple =>
      rec = true
      tuple._2 ! Reconnect(player, tuple._1, _id)
    }
    if (!rec) {
      player ! NotReconnected
    }
  }
}