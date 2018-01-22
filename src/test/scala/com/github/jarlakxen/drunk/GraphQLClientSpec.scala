package com.github.jarlakxen.drunk

import com.github.jarlakxen.drunk.circe._
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
  implicit val episodeDecoder = Decoder.enumDecoder(Episode)
  implicit val humanDecoder: Decoder[Human] = deriveDecoder
  implicit val droidDecoder: Decoder[Droid] = deriveDecoder
  
  implicit val characterDecoder: Decoder[Character] = deriveByTypenameDecoder(
    "Human".decodeAs[Human],
    "Droid".decodeAs[Droid]
  )
  
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

}
