package com.github.jarlakxen.drunk.extensions

import io.circe._, io.circe.parser._, io.circe.generic.semiauto._

object metrics {

  case class GraphQLFieldMetrics(
    count: Long,
    minMs: Long,
    maxMs: Long,
    meanMs: Long,
    p75Ms: Long,
    p95Ms: Long,
    p99Ms: Long)

  case class GraphQLMetricsExtension(
    executionMs: Long,
    validationMs: Long,
    reducersMs: Long,
    query: String,
    types: Map[String, Map[String, GraphQLFieldMetrics]])

  implicit val fieldMetricsDecoder: Decoder[GraphQLFieldMetrics] = deriveDecoder[GraphQLFieldMetrics]
  implicit val metricsExtensionsDecoder: Decoder[GraphQLMetricsExtension] = deriveDecoder[GraphQLMetricsExtension]

}