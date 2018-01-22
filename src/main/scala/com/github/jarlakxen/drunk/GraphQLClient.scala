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

package com.github.jarlakxen.drunk

import scala.concurrent.{ ExecutionContext, Future }

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri

import backend.AkkaHttpBackend
import io.circe._, io.circe.parser._
import sangria._
import sangria.ast.Document
import sangria.parser.{ SyntaxError, QueryParser }

class GraphQLClient private[GraphQLClient] (uri: Uri, options: ClientOptions, backend: AkkaHttpBackend) {
  import GraphQLClient._

  private def query[T, Vars](doc: Document, variables: Option[Vars], operationName: Option[String])(
    implicit
    dec: Decoder[T],
    en: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLQueryResponse[T]] = {

    val fields =
      List("query" -> Json.fromString(doc.toString())) ++
        variables.map("variables" -> en(_)) ++
        operationName.map("operationName" -> Json.fromString(_))

    val body = Json.obj(fields: _*).noSpaces

    for {
      (statusCode, rawBody) <- backend.send(uri, body)
      jsonBody <- Future.fromTry(parse(rawBody).toTry)
      response <- {
        val errors: Option[Future[GraphQLQueryResponse[T]]] =
          readErrors(jsonBody).map(msgs => Future.successful(Left(GraphQLQueryError(msgs.toList, statusCode))))
        val data: Future[GraphQLQueryResponse[T]] =
          Future
            .fromTry {
              jsonBody.hcursor.downField("data").as[T].toTry
            }
            .map(data => Right(GraphQLQueryResult(data)))

        errors getOrElse data
      }
    } yield response
  }

  def query[T](doc: String)(implicit dec: Decoder[T], ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    query(doc, None, None)(dec, null, ec)

  def query[T](
    doc: String,
    operationName: Option[String])(implicit dec: Decoder[T], ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    query(doc, None, operationName)(dec, null, ec)

  def query[T, Vars](doc: String, variables: Vars, operationName: Option[String])(
    implicit
    dec: Decoder[T],
    en: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    for {
      schema <- Future.fromTry(QueryParser.parse(doc.stripMargin))
      result <- query(doc, Some(variables), operationName)
    } yield result

  def query[T](doc: Document)(implicit dec: Decoder[T], ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    query(doc, None, None)(dec, null, ec)

  def query[T](
    doc: Document,
    operationName: Option[String])(implicit dec: Decoder[T], ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    query(doc, None, operationName)(dec, null, ec)

  def query[T, Vars](doc: Document, variables: Vars, operationName: Option[String])(
    implicit
    dec: Decoder[T],
    en: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLQueryResponse[T]] =
    query(doc, Some(variables), operationName)
}

object GraphQLClient {

  type GraphQLQueryResponse[T] = Either[GraphQLQueryError, GraphQLQueryResult[T]]

  def apply(uri: String, backend: AkkaHttpBackend, clientOptions: ClientOptions): GraphQLClient =
    new GraphQLClient(Uri(uri), clientOptions, backend)

  def apply(uri: String, options: ConnectionOptions = ConnectionOptions.Default, clientOptions: ClientOptions = ClientOptions.Default): GraphQLClient =
    new GraphQLClient(Uri(uri), clientOptions, AkkaHttpBackend(options))

  def apply(actorSystem: ActorSystem, uri: String, connectionOptions: ConnectionOptions, clientOptions: ClientOptions): GraphQLClient =
    new GraphQLClient(Uri(uri), clientOptions, AkkaHttpBackend.usingActorSystem(actorSystem, connectionOptions))

  private[GraphQLClient] def readErrors(body: Json): Option[Seq[String]] = {
    val cursor: HCursor = body.hcursor

    for {
      errorsNode <- cursor.downField("errors").focus
      errors <- errorsNode.asArray
    } yield errors.map(_.hcursor.downField("message").as[String].toOption).flatten
  }

}
