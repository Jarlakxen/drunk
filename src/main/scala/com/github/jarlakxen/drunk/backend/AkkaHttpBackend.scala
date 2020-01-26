/*
 * Copyright 2018 Facundo Viale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jarlakxen.drunk.backend

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods, HttpRequest, Uri}

class AkkaHttpBackend private[AkkaHttpBackend] (
  uri: Uri,
  headers: immutable.Seq[HttpHeader],
  httpExt: HttpExt
)(override implicit val as: ActorSystem)
    extends AkkaBackend {

  def send(body: String): Future[(Int, String)] = {
    implicit val ec: ExecutionContext = as.dispatcher

    val req = HttpRequest(HttpMethods.POST, uri, headers, HttpEntity(ContentTypes.`application/json`, body))

    val res = httpExt.singleRequest(req)

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

object AkkaHttpBackend {
  val ContentTypeHeader = "Content-Type"

  def apply(
    uri: Uri,
    headers: immutable.Seq[HttpHeader] = Nil,
    httpExt: Option[HttpExt] = None
  )(implicit as: ActorSystem): AkkaHttpBackend = {

    val http = httpExt.getOrElse { Http(as) }
    new AkkaHttpBackend(uri, headers, http)
  }
}
