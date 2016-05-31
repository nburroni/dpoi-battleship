package util

import org.mongodb.scala.MongoClient

case class MongoUtil(dbName: String){
  var mongoClient: MongoClient = null
  def getDB = {
    try {
      mongoClient = MongoClient()
    }
    catch {
      case e: Exception => e.printStackTrace()
    }
    if (mongoClient != null) mongoClient.getDatabase(dbName) else null
  }
  def close = {
    mongoClient.close()
  }
}