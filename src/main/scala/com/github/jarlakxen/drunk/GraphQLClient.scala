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
import scala.util._

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri

import backend.AkkaHttpBackend
import io.circe._, io.circe.parser._
import sangria._
import sangria.ast.Document
import sangria.introspection._
import sangria.marshalling.circe._
import sangria.parser.{ SyntaxError, QueryParser }

class GraphQLClient private[GraphQLClient] (uri: Uri, options: ClientOptions, backend: AkkaHttpBackend) {
  import GraphQLClient._

  private[drunk] def execute[Res, Vars](doc: Document, variables: Option[Vars], name: Option[String])(
    implicit
    responseDecoder: Decoder[Res],
    variablesEncoder: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLResponse[Res]] =
    execute(GraphQLOperation(doc, variables, name))

  private[drunk] def execute[Res, Vars](op: GraphQLOperation[Res, Vars])(
    implicit
    dec: Decoder[Res],
    ec: ExecutionContext): Future[GraphQLResponse[Res]] = {

    val fields =
      List("query" -> op.docToJson) ++
        op.encodeVariables.map("variables" -> _) ++
        op.name.map("operationName" -> Json.fromString(_))

    val body = Json.obj(fields: _*).noSpaces

    for {
      (statusCode, rawBody) <- backend.send(uri, body)
      jsonBody <- Future.fromTry(parse(rawBody).toTry)
      response <- {
        val errors: Option[Future[GraphQLResponse[Res]]] =
          readErrors(jsonBody).map(msgs => Future.successful(Left(GraphQLResponseError(msgs.toList, statusCode))))

        val data: Future[GraphQLResponse[Res]] =
          Future.fromTry(jsonBody.hcursor.downField("data").as[Res].toTry)
            .map(data => Right(GraphQLResponseData(data)))

        errors getOrElse data
      }
    } yield response
  }

  def query[Res](doc: String)(implicit dec: Decoder[Res], ec: ExecutionContext): Try[GraphQLCursor[Res, Nothing]] =
    query(doc, None, None)(dec, null, ec)

  def query[Res](
    doc: String,
    operationName: String)(implicit dec: Decoder[Res], ec: ExecutionContext): Try[GraphQLCursor[Res, Nothing]] =
    query(doc, None, Some(operationName))(dec, null, ec)

  def query[Res, Vars](doc: String, variables: Vars)(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): Try[GraphQLCursor[Res, Vars]] =
    query(doc, Some(variables), None)

  def query[Res, Vars](doc: String, variables: Option[Vars], operationName: Option[String])(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): Try[GraphQLCursor[Res, Vars]] =
    QueryParser.parse(doc).map(query(_, variables, operationName))

  def query[Res](doc: Document)(implicit dec: Decoder[Res], ec: ExecutionContext): GraphQLCursor[Res, Nothing] =
    query(doc, None, None)(dec, null, ec)

  def query[Res](
    doc: Document,
    operationName: String)(implicit dec: Decoder[Res], ec: ExecutionContext): GraphQLCursor[Res, Nothing] =
    query(doc, None, Some(operationName))(dec, null, ec)

  def query[Res, Vars](doc: Document, variables: Vars)(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): GraphQLCursor[Res, Vars] =
    query(doc, Some(variables), None)

  def query[Res, Vars](doc: Document, variables: Option[Vars], operationName: Option[String])(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): GraphQLCursor[Res, Vars] = {
    var fullDoc = doc
    if (options.addTypename) {
      fullDoc = ast.addTypename(doc)
    }

    val operation: GraphQLOperation[Res, Vars] = GraphQLOperation(doc, variables, operationName)
    val result = execute(operation)
    new GraphQLCursor(this, result, operation)
  }

  def mutate[Res, Vars](doc: Document, variables: Vars)(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLResponse[Res]] =
    mutate(doc, Some(variables), None)

  def mutate[Res, Vars](doc: Document, variables: Vars, operationName: Option[String])(
    implicit
    dec: Decoder[Res],
    en: Encoder[Vars],
    ec: ExecutionContext): Future[GraphQLResponse[Res]] =
    execute(doc, Some(variables), operationName)

  def schema(implicit ec: ExecutionContext): Future[GraphQLResponse[IntrospectionSchema]] =
    execute[Json, Nothing](introspectionQuery, None, None)(implicitly[Decoder[Json]], null, ec).flatMap {
      case Right(GraphQLResponseData(json)) =>
        Future.fromTry(IntrospectionParser.parse(Json.obj("data" -> json))).map { schema =>
          Right(GraphQLResponseData(schema))
        }
      case Left(responseError) =>
        Future.successful(Left(responseError))
    }

}

object GraphQLClient {

  type GraphQLResponse[Res] = Either[GraphQLResponseError, GraphQLResponseData[Res]]

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
