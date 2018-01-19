# Drunk

A simple GraphQL client on top of [Sangria](http://sangria-graphql.org/), [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) and [Circe](https://circe.github.io/circe/).


## Quickstart

Add the following dependency:

```scala
  resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

  "com.github.jarlakxen" %% "drunk" % "1.0.0"
```

Then, import:

```scala
  import com.github.jarlakxen.drunk._
  import io.circe._, io.circe.generic.semiauto._
  import sangria.macros._

  
  val client = GraphQLClient(s"http://$host:$port/api/graphql")

  val doc =
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
      
    client.query[HeroQuery](doc)
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

## Roadmap

- [x] Support Querys
- [ ] Support Mutations

## Contributing

If you have a question, or hit a problem, feel free to ask in the [issues](https://github.com/jarlakxen/drunk/issues)!

Or, if you encounter a bug, something is unclear in the code or documentation, don’t hesitate and open an issue.