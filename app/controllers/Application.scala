package controllers

import play.api.mvc._

class Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  def socket = Action {
    Ok(views.html.socket())
  }

  def game = Action {
    Ok(views.html.game())
  }

}