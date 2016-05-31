package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import play.api.libs.json.Json
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.api.libs.streams._

class Controller4 @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller{
  import akka.actor._

  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]

  implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]

  class MyWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case InEvent(msg) => out ! OutEvent(msg)
    }
  }

  object MyWebSocketActor {
    def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  }

  def socket = WebSocket.accept[InEvent, OutEvent] { request =>
    ActorFlow.actorRef(out => MyWebSocketActor.props(out))
  }

  def socketUrl = Action { request =>
    val url = "ws://" + request.host + routes.Controller4.socket.url
    Ok(url)
  }

}

case class InEvent(msg: String)
case class OutEvent(msg: String)