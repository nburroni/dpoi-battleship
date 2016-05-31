package controllers

import org.mongodb.scala.{Completed, Document, Observer}
import org.mongodb.scala.model.Filters._
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import util._

case class User(id: Long, name: String)

class Users extends Controller {

  def logUser() = Action {
    request =>
//      implicit val reads = Json.reads[User]
      var response = Ok("")
      val post = request.body.asJson.orNull
      val mongoUtil = new MongoUtil("battleship")
      val db = mongoUtil.getDB
      if (db == null) {
        //TODO
        println("null db")
      }
      val collection = db.getCollection("users")
      var user: Document = null
      collection.find(equal("fbid", (post \ "id").asInstanceOf[Long])).subscribe(
        new Observer[Document] {
          override def onError(e: Throwable): Unit = println(e.getMessage)

          override def onComplete(): Unit = {
            println("Completed find")
          }

          override def onNext(result: Document): Unit = {
            user = result
          }
        })
      if (user != null){
        Ok(Json.toJson(Map("status" -> "200", "message" -> "user already registered")))
      }else{
        val doc = Document("fbid" -> (post \ "id").asInstanceOf[Long], "name" -> (post \ "name").asInstanceOf[String])
        collection.insertOne(doc).subscribe(
          new Observer[Completed] {
            override def onError(e: Throwable): Unit = {
              response = BadRequest(Json.toJson(Map("status" -> "400", "message" -> "error inserting user")))
            }

            override def onComplete(): Unit = println("Insertion completed")

            override def onNext(result: Completed): Unit = {
              response = Ok(Json.toJson(Map("status" -> "200", "message" -> "inserted successfully")))
            }
          }
        )
        mongoUtil.close
        response
      }
  }
}