package game

/**
  * Created by nico on 28/05/16.
  */

import akka.actor.{ActorSystem, Props, ActorRef}
import com.mongodb.client.result.UpdateResult
import controllers.Messages.{MatchData, NotReconnected, Reconnect}
import org.mongodb.scala.bson._
import org.mongodb.scala.{Completed, Document}
import play.api._
import util.MongoUtil
import org.mongodb.scala.model.Filters._

import scala.collection.immutable.Queue


object GameManager extends GlobalSettings {
  def saveData(_id: String, data: MatchData) = {
    val mongoUtil = MongoUtil("battleship")
    val db = mongoUtil.getDB
    if (db != null) {
      val collection = db.getCollection("users")
      collection.find(equal("_id", _id)).first().subscribe {
        user: Document =>
          val wins = if (data.won) 1 else 0
          val losses = if (wins == 1) 0 else 1
          val prevWins = user.get("wins").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val prevLosses = user.get("losses").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val prevHits = user.get("hits").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val prevMisses = user.get("misses").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val prevTime = user.get("time").getOrElse(BsonInt64(0)).asInstanceOf[BsonInt64].getValue
          val name = user.get("name").getOrElse(BsonString("")).asInstanceOf[BsonString].getValue


          val newDocument = Document(
            "_id" -> _id,
            "name" -> name,
            "wins" -> (prevWins + wins),
            "losses" -> (prevLosses + losses),
            "hits" -> (prevHits + data.hits),
            "misses" -> (prevMisses + data.misses),
            "time" -> (prevTime + data.time)
          )
          collection.replaceOne(equal("_id", _id), newDocument) subscribe{
            r: UpdateResult =>
              println(r.getMatchedCount)
          }
      }
    }
  }

  def successfulReconnection(prevId: String) = {
    reconnectPlayers -= prevId
  }

  private var reconnectPlayers: Map[String, (ActorRef, ActorRef)] = Map()
  private var pendingPlayers = Queue[ActorRef]()


  def setReconnect(_id: String, player: ActorRef, game: ActorRef) = {
    if (!reconnectPlayers.contains(_id)) {
      reconnectPlayers += (_id ->(player, game))
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