package controllers

import javax.inject.Inject

import akka.actor._
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.libs.streams._
import util.Global

class SocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {

  import Messages._

  implicit val fireAction = Json.format[Fire]


  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  class MyWebSocketActor(out: ActorRef) extends Actor {
    var rival: ActorRef = null

    def receive = {
      case e: InEvent => e toMessage match {
        case SearchGame =>
          Global getFirstPendingPlayer match {
            case Some(v) =>
              rival = v
              v ! MatchPlayer(self)
              out ! OutEvent("matched-player")
            case None =>
              Global addPlayer self
              out ! OutEvent("searching-game")
          }
        case Fire(x, y) =>
          //          if miss
          out ! OutEvent("miss", Some(Fire(x, y)))
          rival ! InEvent("miss", Some(Fire(x, y)))
        case Miss(x, y) =>
          out ! OutEvent("miss-received", Some(Fire(x, y)))
      }
      case MatchPlayer(actor) =>
        rival = actor
        out ! OutEvent("matched-player")
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

object Messages {

  case class InEvent(action: String, fire: Option[Fire] = None) {
    def toMessage: Message = {
      action match {
        case "search-game" => SearchGame
        case "fire" => fire.getOrElse(InvalidAction("invalid-fire"))
        case "miss" => fire.map((f: Fire) => Miss(f.x, f.y)).getOrElse(InvalidAction("invalid-miss"))
        //        case "fire-to" => FireTo(userKey, fire.get)
      }
    }
  }

  case class OutEvent(msg: String, fire: Option[Fire] = None)

  trait Message

  case class InvalidAction(msg: String) extends Message

  case class MatchPlayer(actor: ActorRef) extends Message

  case object SearchGame extends Message

  case class Fire(x: Int, y: Int) extends Message

  case class Miss(x: Int, y: Int) extends Message

  //  case class FireTo(userKey: String, x: Int, y: Int) extends Message

}
