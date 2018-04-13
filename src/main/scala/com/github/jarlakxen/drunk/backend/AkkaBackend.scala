package com.github.jarlakxen.drunk.backend

import java.io.UnsupportedEncodingException

import akka.actor.ActorSystem
import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.stream.ActorMaterializer
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

trait AkkaBackend {
  implicit val as: ActorSystem
  implicit val mat: ActorMaterializer

  def send(body: String): Future[(Int, String)]

  protected def encodingFromContentType(ct: String): Option[String] =
    ct.split(";").map(_.trim.toLowerCase).collectFirst {
      case s if s.startsWith("charset=") => s.substring(8)
    }

  protected def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip     => Gzip
      case HttpEncodings.deflate  => Deflate
      case HttpEncodings.identity => NoCoding
      case ce =>
        throw new UnsupportedEncodingException(s"Unsupported encoding: $ce")
    }

    decoder.decodeMessage(response)
  }

  protected def bodyToString(hr: HttpResponse, charsetFromHeaders: String): Future[String] = {
    implicit val ec: ExecutionContext = as.dispatcher

    hr.entity.dataBytes
      .runFold(ByteString.empty)(_ ++ _)
      .map(_.decodeString(charsetFromHeaders))
  }
}
