package com.github.jarlakxen.drunk

import com.github.jarlakxen.drunk.circe._
import com.github.jarlakxen.drunk.extensions._
import com.github.jarlakxen.drunk.extensions.metrics._
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._
import sangria.macros._

object Episode extends Enumeration {
  val NEWHOPE, EMPIRE, JEDI = Value
}

trait Character {
  def id: String
  def name: Option[String]
  def friends: List[String]
  def appearsIn: List[Episode.Value]
}

case class Human(
  id: String,
  name: Option[String],
  friends: List[String],
  appearsIn: List[Episode.Value],
  homePlanet: Option[String]) extends Character

case class Droid(
  id: String,
  name: Option[String],
  friends: List[String],
  appearsIn: List[Episode.Value],
  primaryFunction: Option[String]) extends Character

case class HeroQuery(hero: Character)

class GraphQLClientSpec extends Spec with TestHttpServer {
  implicit val episodeDecoder = Decoder.decodeEnumeration(Episode)
  implicit val humanDecoder: Decoder[Human] = deriveDecoder
  implicit val droidDecoder: Decoder[Droid] = deriveDecoder

  implicit val characterDecoder: Decoder[Character] = deriveByTypenameDecoder(
    "Human".decodeAs[Human],
    "Droid".decodeAs[Droid])

  implicit val heroQueryDecoder: Decoder[HeroQuery] = deriveDecoder

  val serverRoutes = {
    import akka.http.scaladsl.server.Directives._
    pathPrefix("api") {
      post {
        path("graphql" / "test1") {
          complete {
            """
              {
                "data": {
                  "hero": {
                    "__typename": "Droid",
                    "id": "2001",
                    "name": "R2-D2",
                    "friends": [
                      "Luke Skywalker",
                      "Han Solo",
                      "Leia Organa"
                    ],
                    "appearsIn": [
                      "NEWHOPE",
                      "EMPIRE",
                      "JEDI"
                    ]
                  }
                }
              }
            """
          }
        } ~
          path("graphql" / "test2") {
            complete {
              """
              {
               "data":null,
               "errors":[
                 {
                   "message":"Cannot query field 'test'",
                   "locations":[{"line":14,"column":7}]
                 }
                ]
              }
            """
            }
          } ~
          path("graphql" / "test3") {
            complete {
              """
              {
                "data": {
                  "human": {
                    "name": "Luke Skywalker",
                    "appearsIn": ["NEWHOPE", "EMPIRE", "JEDI"],
                    "friends": [
                      {"name": "Han Solo"},
                      {"name": "Leia Organa"},
                      {"name": "C-3PO"},
                      {"name": "R2-D2"}
                    ]
                  }
                },
                "extensions": {
                  "metrics": {
                    "executionMs": 362,
                    "validationMs": 0,
                    "reducersMs": 0,
                    "query": "",
                    "types": {
                      "Human": {
                        "friends": {
                          "count": 1, "minMs": 358, "maxMs": 358, "meanMs": 358,
                          "p75Ms": 358, "p95Ms": 358, "p99Ms": 358
                        },
                        "appearsIn": {
                          "count": 1, "minMs": 216, "maxMs": 216, "meanMs": 216,
                          "p75Ms": 216, "p95Ms": 216, "p99Ms": 216
                        },
                        "name": {
                          "count": 3, "minMs": 0, "maxMs": 0, "meanMs": 0,
                          "p75Ms": 0, "p95Ms": 0, "p99Ms": 0
                        }
                      },
                      "Query": {
                        "human": {
                          "count": 1, "minMs": 2, "maxMs": 2, "meanMs": 2,
                          "p75Ms": 2, "p95Ms": 2, "p99Ms": 2
                        }
                      },
                      "Droid": {
                        "name": {
                          "count": 2, "minMs": 0, "maxMs": 0, "meanMs": 0,
                          "p75Ms": 0, "p95Ms": 0, "p99Ms": 0
                        }
                      }
                    }
                  }
                }
              }
              """
            }
          }
      }
    }
  }

  "GraphQLClient" should "execute a successful query" in {
    val client = GraphQLClient(s"http://$host:$port/api/graphql/test1")

    val doc =
      graphql"""
        query HeroAndFriends {
          hero {
            __typename
            id
            name
            friends {
              name
            }
            appearsIn
          }
        }
      """

    val expected = HeroQuery(Droid("2001", Some("R2-D2"), List("Luke Skywalker", "Han Solo", "Leia Organa"), List(Episode.NEWHOPE, Episode.EMPIRE, Episode.JEDI), None))
    client.query[HeroQuery](doc).result.futureValue shouldBe Right(GraphQLResponseData(expected))
  }

  it should "fail to execute query" in {
    val client = GraphQLClient(s"http://$host:$port/api/graphql/test2")

    val doc =
      graphql"""
        query HeroAndFriends {
          hero {
            test
          }
        }
      """

    client.query[Human](doc).result.futureValue shouldBe Left(GraphQLResponseError(List("Cannot query field 'test'"), 200))
  }

  it should "get profiling metrics" in {
    val client = GraphQLClient(s"http://$host:$port/api/graphql/test3")

    val doc =
      graphql"""
        {
          human(id: "123") {
            name
            appearsIn
            friends
          }
        }
      """

    val types = Map(
      "Human" ->
        Map(
          "friends" -> GraphQLFieldMetrics(1, 358, 358, 358, 358, 358, 358),
          "appearsIn" -> GraphQLFieldMetrics(1, 216, 216, 216, 216, 216, 216),
          "name" -> GraphQLFieldMetrics(3, 0, 0, 0, 0, 0, 0)),
      "Query" ->
        Map("human" -> GraphQLFieldMetrics(1, 2, 2, 2, 2, 2, 2)),
      "Droid" -> Map("name" -> GraphQLFieldMetrics(2, 0, 0, 0, 0, 0, 0)))
    val metrics = GraphQLMetricsExtension(362, 0, 0, "", types)
    val extensions = GraphQLExtensions(Some(metrics), None)

    client.query[Human](doc).extensions.futureValue shouldBe extensions
  }

}
