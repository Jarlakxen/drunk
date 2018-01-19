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

import java.io.{File, IOException, UnsupportedEncodingException}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.{ClientTransport, Http, HttpsConnectionContext}
import akka.http.scaladsl.coding.{Deflate, Gzip, NoCoding}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.headers.HttpEncodings
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import com.github.jarlakxen.drunk._

class AkkaHttpBackend private[AkkaHttpBackend] (
  actorSystem: ActorSystem,
  terminateActorSystemOnClose: Boolean,
  opts: ConnectionOptions,
  customHttpsContext: Option[HttpsConnectionContext],
  customConnectionPoolSettings: Option[ConnectionPoolSettings],
  customLog: Option[LoggingAdapter]
) {

  private implicit val as: ActorSystem = actorSystem
  private implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val http = Http()

  private val connectionSettings =
    ClientConnectionSettings(actorSystem)
      .withConnectingTimeout(opts.connectionTimeout)

  private val connectionPoolSettings = {
    val base = customConnectionPoolSettings.getOrElse(ConnectionPoolSettings(actorSystem))
    opts.proxy match {
      case None => base
      case Some(p) =>
        base.withTransport(ClientTransport.httpsProxy(p.inetSocketAddress))
    }
  }

  def send(uri: Uri, body: String): Future[(Int, String)] = {
    implicit val ec: ExecutionContext = as.dispatcher

    val req = HttpRequest(HttpMethods.POST, uri, Nil, HttpEntity(ContentTypes.`application/json`, body))

    val res = http.singleRequest(
      req,
      settings = connectionPoolSettings,
      connectionContext = customHttpsContext.getOrElse(http.defaultClientHttpsContext),
      log = customLog.getOrElse(actorSystem.log)
    )

    res.flatMap { hr =>
      val code = hr.status.intValue()

      val charsetFromHeaders =
        encodingFromContentType(hr.entity.contentType.toString)

      if (code >= 200 && code < 300) {
        bodyFrom(decodeResponse(hr), charsetFromHeaders).map((code, _))
      } else {
        Future.failed(new RuntimeException(s"${uri.toString} return $code"))
      }
    }
  }

  private def encodingFromContentType(ct: String): Option[String] =
    ct.split(";").map(_.trim.toLowerCase).collectFirst {
      case s if s.startsWith("charset=") => s.substring(8)
    }

  private def decodeResponse(response: HttpResponse): HttpResponse = {
    val decoder = response.encoding match {
      case HttpEncodings.gzip     => Gzip
      case HttpEncodings.deflate  => Deflate
      case HttpEncodings.identity => NoCoding
      case ce =>
        throw new UnsupportedEncodingException(s"Unsupported encoding: $ce")
    }

    decoder.decodeMessage(response)
  }

  private def bodyFrom(hr: HttpResponse, charsetFromHeaders: Option[String]): Future[String] = {
    implicit val ec: ExecutionContext = as.dispatcher

    val asByteArray: Future[Array[Byte]] =
      hr.entity.dataBytes
        .runFold(ByteString.empty)(_ ++ _)
        .map(_.toArray[Byte])

    asByteArray.map(new String(_, charsetFromHeaders.getOrElse("utf-8")))
  }

  def close(): Unit =
    if (terminateActorSystemOnClose) actorSystem.terminate()

}

object AkkaHttpBackend {
  val ContentTypeHeader = "Content-Type"

  def apply(
    options: ConnectionOptions = ConnectionOptions.Default,
    customHttpsContext: Option[HttpsConnectionContext] = None,
    customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
    customLog: Option[LoggingAdapter] = None
  ): AkkaHttpBackend =
    new AkkaHttpBackend(
      ActorSystem("sttp"),
      terminateActorSystemOnClose = true,
      options,
      customHttpsContext,
      customConnectionPoolSettings,
      customLog
    )

  /**
    * @param actorSystem The actor system which will be used for the http-client
    *                    actors.
    * @param ec The execution context for running non-network related operations,
    *           e.g. mapping responses. Defaults to the global execution
    *           context.
    */
  def usingActorSystem(
    actorSystem: ActorSystem,
    options: ConnectionOptions = ConnectionOptions.Default,
    customHttpsContext: Option[HttpsConnectionContext] = None,
    customConnectionPoolSettings: Option[ConnectionPoolSettings] = None,
    customLog: Option[LoggingAdapter] = None
  ): AkkaHttpBackend =
    new AkkaHttpBackend(
      actorSystem,
      terminateActorSystemOnClose = false,
      options,
      customHttpsContext,
      customConnectionPoolSettings,
      customLog
    )
}
