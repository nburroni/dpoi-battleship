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


  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  class MyWebSocketActor(out: ActorRef) extends Actor {
    var game: Option[ActorRef] = None

    def receive = {
      case e: InEvent => e toMessage match {
        case SearchGame => GameManager addPlayer self
        case f: Fire => game.foreach(_ ! f)
      }

      case MatchGame(gameActor) =>
        this.game = Some(gameActor)
        out ! OutEvent("matched-player")

      case MissedShot(x, y) =>
        out ! OutEvent("miss", Some(Fire(x, y)))

      case OpponentMissed(x, y) =>
        out ! OutEvent("miss-received", Some(Fire(x, y)))

      case YourTurn =>
        out ! OutEvent("my-turn")

      case OpponentTurn =>
        out ! OutEvent("their-turn")

    }
  }

  object MyWebSocketActor {
    def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  }

  def socket = WebSocket.accept[InEvent, OutEvent] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out))
  }

  def socketUrl = Action { request =>
    val url = "ws://" + request.host + routes.SocketController.socket.url
    Ok(url)
  }

}