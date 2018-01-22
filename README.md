# Drunk

A simple GraphQL client on top of [Sangria](http://sangria-graphql.org/), [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) and [Circe](https://circe.github.io/circe/).


## Quickstart

Add the following dependency:

```scala
  resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

  "com.github.jarlakxen" %% "drunk" % "2.1.0"
```

Then, import:

```scala
  import com.github.jarlakxen.drunk._
  import io.circe._, io.circe.generic.semiauto._
  import sangria.macros._

  
  val client = GraphQLClient(s"http://$host:$port/api/graphql")

  val query =
    graphql"""
      query HeroAndFriends {
        hero {
          id
          name
          friends {
            name
          }
          appearsIn
        }
      }
    """
      
    client.query[HeroQuery](query)
```

### Mutations


```scala
  import com.github.jarlakxen.drunk._
  import io.circe._, io.circe.generic.semiauto._
  import sangria.macros._

  case class User(id: String, name: String)
  
  val client = GraphQLClient(s"http://$host:$port/api/graphql")

  val mutation =
    graphql"""
      mutation($user1: String!, $user2: String!) {
        user1: newUser(name: $user1) {
          id
          name
        }
        user2: newUser(name: $user2) {
          id
          name
        }
      }
    """
    val result: Future[GraphQLResponse[Map[String, User]]] = 
      client.query(mutation, Map("user1" -> "123", "user2" -> "456"))
```

### Schema Introspection

```scala
  import com.github.jarlakxen.drunk._
  import sangria.introspection.IntrospectionSchema

  val client = GraphQLClient(s"http://$host:$port/api/graphql")
      
  val result: Future[GraphQLResponse[IntrospectionSchema]] = client.schema
```

## Typename Derive

```scala
  import com.github.jarlakxen.drunk.circe._
  import io.circe._, io.circe.generic.semiauto._

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
    
  implicit val humanDecoder: Decoder[Human] = deriveDecoder
  implicit val droidDecoder: Decoder[Droid] = deriveDecoder
  
  implicit val characterDecoder: Decoder[Character] = deriveByTypenameDecoder(
    "Human".decodeAs[Human], // for __typename: 'Human' is going to use humanDecoder
    "Droid".decodeAs[Droid] // for __typename: 'Droid' is going to use droidDecoder
  )
```

## Contributing

If you have a question, or hit a problem, feel free to ask in the [issues](https://github.com/jarlakxen/drunk/issues)!

Or, if you encounter a bug, something is unclear in the code or documentation, don’t hesitate and open an issue.