package com.github.jarlakxen.drunk.backend

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class AkkaConnectionBackend  private[AkkaConnectionBackend] (
  uri: Uri,
  flow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]],
  headers: immutable.Seq[HttpHeader]
)(override implicit val as: ActorSystem, override implicit val mat: ActorMaterializer)
   extends AkkaBackend {

  def send(body: String): Future[(Int, String)] = {
    implicit val ec: ExecutionContext = as.dispatcher

    val req = HttpRequest(
      method = HttpMethods.POST,
      uri = uri,
      headers = headers,
      entity = HttpEntity(ContentTypes.`application/json`, body)
    )

    val res = Source.single(req).via(flow).runWith(Sink.head)

    res.flatMap { hr =>
      val code = hr.status.intValue()

      val charsetFromHeaders = encodingFromContentType(hr.entity.contentType.toString).getOrElse("utf-8")
      val decodedResponse = decodeResponse(hr)
      val stringBody = bodyToString(decodedResponse, charsetFromHeaders)

      if (code >= 200 && code < 300) {
        stringBody.map { body =>
          hr.discardEntityBytes()
          (code, body)
        }
      } else {
        stringBody.flatMap { body =>
          hr.discardEntityBytes()
          Future.failed(new RuntimeException(s"${uri.toString} return $code with body: $body"))
        }
      }
    }
  }

}

object AkkaConnectionBackend {

  def apply(uri: Uri,
             flow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]],
             headers: immutable.Seq[HttpHeader] = Nil
           )( implicit  as: ActorSystem,  mat: ActorMaterializer): AkkaConnectionBackend =
    new AkkaConnectionBackend(uri, flow, headers)

}