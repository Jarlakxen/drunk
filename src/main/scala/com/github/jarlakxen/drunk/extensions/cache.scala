package com.github.jarlakxen.drunk.extensions

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

object cache {

  object GraphQLCacheControlScope extends Enumeration {
    val PRIVATE, PUBLIC = Value
  }

  case class GraphQLCacheControlHint(path: List[String], maxAge: Long, scope: Option[GraphQLCacheControlScope.Value])
  case class GraphQLCacheControlExtension(version: Int, hints: List[GraphQLCacheControlHint])

  implicit val cacheControlScopeDecoder: Decoder[GraphQLCacheControlScope.Value] = Decoder.decodeEnumeration(GraphQLCacheControlScope)
  implicit val cacheControlHintDecoder: Decoder[GraphQLCacheControlHint] = deriveDecoder[GraphQLCacheControlHint]
  implicit val cacheControlExtensionDecoder: Decoder[GraphQLCacheControlExtension] = deriveDecoder[GraphQLCacheControlExtension]

}