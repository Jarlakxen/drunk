package com.github.jarlakxen.drunk

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

package object extensions {

  import metrics._
  import cache._

  case class GraphQLExtensions(
    metrics: Option[GraphQLMetricsExtension],
    cacheControl: Option[GraphQLCacheControlExtension])

  implicit val graphQLExtensionsDecoder: Decoder[GraphQLExtensions] = deriveDecoder[GraphQLExtensions]

  val NoExtensions = GraphQLExtensions(None, None)
}