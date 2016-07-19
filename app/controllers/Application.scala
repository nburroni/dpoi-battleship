package controllers

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def game = Action {
    Ok(views.html.game())
  }

}