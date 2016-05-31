package controllers

import javax.inject.Inject

import akka.actor._
import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.libs.streams._

class SocketController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller{

  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  class MyWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case InEvent(action, msg) => out ! OutEvent(msg)
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

case class InEvent(action: String, msg: String)
case class OutEvent(msg: String)