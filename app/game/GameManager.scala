package game

/**
  * Created by nico on 28/05/16.
  */

import akka.actor.{ActorSystem, Props, ActorRef}
import com.mongodb.client.result.UpdateResult
import controllers.Messages.{UserData, MatchData, NotReconnected, Reconnect}
import controllers.PlayerActor
import org.mongodb.scala.bson._
import org.mongodb.scala.{Completed, Document}
import play.api._
import util.MongoUtil
import org.mongodb.scala.model.Filters._

import scala.collection.immutable.Queue


object GameManager extends GlobalSettings {

  def getStats(_id: String, player: ActorRef) = {
    val mongoUtil = MongoUtil("battleship")
    val db = mongoUtil.getDB
    if (db != null) {
      val collection = db.getCollection("users")
      collection.find(equal("_id", _id)).first().subscribe{
        user: Document =>
          val wins = user.get("wins").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val losses = user.get("losses").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val hits = user.get("hits").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          val misses = user.get("misses").getOrElse(BsonInt32(0)).asInstanceOf[BsonInt32].getValue
          player ! UserData(wins, losses, hits, misses)
      }
//      val gamesCollection = db.getCollection("games")
//      gamesCollection.find().toFuture().map{
//        case Seq(doc) =>
//          doc.get("player") match {
//            case Some(playerId) =>
//          }
//      }
    }
  }

  def saveData(_id: String, data: MatchData, rivalId: String) = {
    val mongoUtil = MongoUtil("battleship")
    val db = mongoUtil.getDB
    if (db != null) {
      val collection = db.getCollection("users")
      var saved, inserted = false
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
              saved = true
              println(r.getMatchedCount)
              if (inserted) mongoUtil.close
          }
      }
//      val gamesCollection = db.getCollection("games")
//      val newGame = Document("player" -> _id, "rival" -> rivalId, "duration" -> data.time, "won" -> data.won, "hits" -> data.hits, "misses" -> data.misses)
//      gamesCollection.insertOne(newGame) subscribe{
//        (c:Completed) =>
//          if (saved) mongoUtil.close
//          inserted = true
//      }
    }
  }

  def successfulReconnection(prevId: String) = {
    reconnectPlayers -= prevId
  }

  private var reconnectPlayers: Map[String, (ActorRef, ActorRef)] = Map()
  private var pendingPlayers = Queue[PlayerActor]()


  def setReconnect(_id: String, player: ActorRef, game: ActorRef) = {
    if (!reconnectPlayers.contains(_id)) {
      reconnectPlayers += (_id ->(player, game))
    }
  }

  def addPlayer(player: PlayerActor) = {
    getFirstPendingPlayer match {
      case Some(rival) =>
        if (rival.id == player.id) {
          pendingPlayers = pendingPlayers enqueue player
        } else {
          val props: Props = Props(new GameActor(rival, player))
          ActorSystem("mySystem").actorOf(props, "gameActor")
        }
      case None =>
        pendingPlayers = pendingPlayers enqueue player
    }
  }

  def getFirstPendingPlayer: Option[PlayerActor] = {
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