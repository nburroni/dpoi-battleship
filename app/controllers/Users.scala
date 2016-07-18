package controllers

import controllers.Messages.MatchData
import org.mongodb.scala.{Completed, Document}
import org.mongodb.scala.model.Filters._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import util._

case class User(id: String, name: String)

class Users extends Controller {

  def logUser() = Action { request =>
    implicit val reads = Json.reads[User]

    request.body.asJson.fold(BadRequest("Bad json")) { userJson: JsValue =>
      Json.fromJson(userJson).asOpt.fold(BadRequest("Bad user")) { user: User =>
        val mongoUtil = MongoUtil("battleship")
        val db = mongoUtil.getDB
        if (db == null) {
          InternalServerError("DB does not exist")
        } else {
          val collection = db.getCollection("users")
          val userDoc = Document(
            "_id" -> user.id,
            "name" -> user.name,
            "wins" -> 0,
            "losses" -> 0,
            "hits" -> 0,
            "misses" -> 0,
            "time" -> 0l
          )
          collection.count(equal("_id", user.id)) subscribe { count: Long =>
            if (count == 0l) collection.insertOne(userDoc) subscribe {
              (c: Completed) => mongoUtil.close
            } else {
              mongoUtil.close
            }
          }

          Ok("Success!").withSession("_id" -> user.id)
        }
      }
    }
  }

  def profileView() = Action {
    Ok(views.html.profile())
  }
}