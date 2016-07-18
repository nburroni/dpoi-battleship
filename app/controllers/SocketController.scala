package controllers

import javax.inject.Inject

import akka.actor._
import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.Messages._
import game.{ReconnectData, PlayerData, GameManager}
import play.api.libs.json.Json
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.libs.streams._

class SocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {

  import Messages._

  implicit val fireAction = Json.format[Fire]
  implicit val coordsAction = Json.format[Coords]
  implicit val placementAction = Json.format[ShipPlacement]
  implicit val fireMap = Json.format[FireMap]
  implicit val recDataFormat = Json.format[ReconnectData]
  implicit val sunkFormat = Json.format[Sunk]
  implicit val matchFormat = Json.format[MatchData]

  implicit val inEventFormat = Json.format[ActionIn]
  implicit val outEventFormat = Json.format[ActionOut]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[ActionIn, ActionOut]


  object PlayerActor {
    def props(out: ActorRef, _id: String) = Props(new PlayerActor(out, _id))
  }

  def socket = WebSocket.accept[ActionIn, ActionOut] { request =>
    ActorFlow.actorRef(out => PlayerActor.props(out, request.session.get("_id").getOrElse("")))
  }

  def socketUrl = Action { request =>
    val url = "ws://" + request.host + routes.SocketController.socket.url
    Ok(url)
  }

}


class PlayerActor(out: ActorRef, _id: String) extends Actor {
  var gameOption: Option[ActorRef] = None
  def id = _id

  def receive = {
    case e: ActionIn => e toMessage match {
      case SearchGame => GameManager.addPlayer(this)
      case f: Fire => gameOption foreach (_ ! f)
      case p: PlacedShips => gameOption foreach (_ ! p)
      case SavePlayer =>
        gameOption foreach { act =>
          GameManager setReconnect(_id, self, act)
        }
      case TryReconnect => GameManager tryReconnect(_id, self)
      case m: MatchData => GameManager saveData(_id, m)
    }

    case NotReconnected =>
      out ! ActionOut("not-reconnected")

    case Reconnected(gameActor, pData, rivalData) =>
      this.gameOption = Some(gameActor)
      val shipList = pData.shipsOption.getOrElse(Map()).keys.toList
      val myFires = getFires(rivalData)
      val oppFires = getFires(pData)
      out ! ActionOut("reconnected", data = Some(ReconnectData(pData.hasTurn, oppFires, shipList, myFires)))

    case MatchGame(gameActor, id) =>
      this.gameOption = Some(gameActor)
      out ! ActionOut("matched-player", oppId = Some(id))

    case GameReady =>
      out ! ActionOut("game-ready")

    case MissedShot(x, y) =>
      out ! ActionOut("miss", Some(Fire(x, y)))

    case OpponentMissed(x, y) =>
      out ! ActionOut("miss-received", Some(Fire(x, y)))

    case HitShot(x, y) =>
      out ! ActionOut("hit", Some(Fire(x, y)))

    case OpponentHit(x, y) =>
      out ! ActionOut("hit-received", Some(Fire(x, y)))

    case YourTurn =>
      out ! ActionOut("my-turn")

    case OpponentTurn =>
      out ! ActionOut("their-turn")

    case SunkShip(x, y) =>
      out ! ActionOut("sunk-ship", Some(Fire(x, y)))

    case YouLost(x, y) =>
      out ! ActionOut("lost-match", Some(Fire(x, y)))

    case YouWon(x, y) =>
      out ! ActionOut("won-match", Some(Fire(x, y)))

    case OpponentSunkShip(x, y) =>
      out ! ActionOut("sunk-received", Some(Fire(x, y)))
  }

  def getFires(data: PlayerData) = {
    val fires = data.gridOption.getOrElse(List()).map{
      coords: Coords=>
        val hit: Boolean = data.shipsOption.fold(false) {
          case placements: Map[ShipPlacement, Sunk] =>
            placements.map {
              case (placement, sunk) => placement.contains(coords)
            }.fold(false)(_ || _)
        }
        FireMap(coords, hit)
    }
    fires
  }
}

case class FireMap(coords: Coords, hit: Boolean)
