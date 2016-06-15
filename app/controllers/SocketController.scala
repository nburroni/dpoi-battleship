package controllers

import javax.inject.Inject

import akka.actor._
import akka.actor.ActorSystem
import akka.stream.Materializer
import game.GameManager
import play.api.libs.json.Json
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.libs.streams._

class SocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {

  import Messages._

  implicit val fireAction = Json.format[Fire]
  implicit val coordsAction = Json.format[Coords]
  implicit val placementAction = Json.format[ShipPlacement]

  implicit val inEventFormat = Json.format[ActionIn]
  implicit val outEventFormat = Json.format[ActionOut]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[ActionIn, ActionOut]

  class PlayerActor(out: ActorRef) extends Actor {
    var gameOption: Option[ActorRef] = None

    def receive = {
      case e: ActionIn => e toMessage match {
        case SearchGame => GameManager addPlayer self
        case f: Fire => gameOption foreach (_ ! f)
        case p: PlacedShips => gameOption foreach (_ ! p)
      }

      case MatchGame(gameActor) =>
        this.gameOption = Some(gameActor)
        out ! ActionOut("matched-player")

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
        out ! ActionOut("sunk-ship", Some(Fire(x,y)))
    }
  }

  object PlayerActor {
    def props(out: ActorRef) = Props(new PlayerActor(out))
  }

  def socket = WebSocket.accept[ActionIn, ActionOut] { request =>
    ActorFlow.actorRef(out => PlayerActor.props(out))
  }

  def socketUrl = Action { request =>
    val url = "ws://" + request.host + routes.SocketController.socket.url
    Ok(url)
  }

}