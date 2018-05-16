package com.github.jarlakxen.drunk.backend

import akka.http.scaladsl.model.Uri
import scala.concurrent.Future

trait GraphQLBackend {
  def send(uri: Uri, body: String): Future[(Int, String)]
}
