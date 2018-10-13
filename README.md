# Drunk

A simple GraphQL client on top of [Sangria](http://sangria-graphql.org/), [Akka HTTP](https://doc.akka.io/docs/akka-http/current/) and [Circe](https://circe.github.io/circe/).


## Quickstart

Add the following dependency:

```scala
  resolvers += Resolver.bintrayRepo("jarlakxen", "maven")

  "com.github.jarlakxen" %% "drunk" % "2.5.0"
```

Then, import:


```scala
  import com.github.jarlakxen.drunk._
  import io.circe._, io.circe.generic.semiauto._
  import sangria.macros._
```

There are three ways to create a `GraphQLClient`:

1) As Akka Https flow connection

```
 import akka.http.scaladsl.model.Uri
 
 val uri: Uri = Uri(s"https://$host:$port/api/graphql")

 val http: HttpExt = Http()
 val flow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] = http.outgoingConnectionHttps(uri.authority.host.address(), uri.effectivePort)
 val client = GraphQLClient(uri, flow, clientOptions = ClientOptions.Default, headers = Nil)

```

2) As Akka Http flow connection

```
 import akka.http.scaladsl.model.Uri
 
 val uri: Uri = Uri(s"http://$host:$port/api/graphql")

 val http: HttpExt = Http()
 val flow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] = http.outgoingConnection(uri.authority.host.address(), uri.effectivePort)
 val client = GraphQLClient(uri, flow, clientOptions = ClientOptions.Default, headers = Nil)

```

3) As Akka Http single request

```  
  val client = GraphQLClient(s"http://$host:$port/api/graphql")
```

Then, query:

```
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
      
  val cursor: GraphQLCursor = client.query[HeroQuery](query)
  val data: Future[GraphQLResponse[HeroQuery]] = cursor.result
```

#### Working with the `GraphQLCursor`

```scala

  type HerosQuery = Map[String, List[Hero]]
  
  case class Pagination(offset: Int, size: Int)

  val query =
    graphql"""
      query Heros($offset: Int, $size: Int) {
        heros(offset: $offset, size: $size) {
          id
          name
          friends {
            name
          }
          appearsIn
        }
      }
    """
      
  val page1: GraphQLCursor[HerosQuery, Pagination] = 
    client.query(query, Pagination(0, 10))
  val page2: GraphQLCursor[HerosQuery, Pagination] = 
    page1.fetchMore(lastPage => lastPage.copy(offset = lastPage.offset + lastPage.size ) )
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

It's very common in GraphQL to have response with polymorphic objects in the responses. One way to discriminate the type of object is to check the `__typename` field. For that purpose there an special derive decoder in `com.github.jarlakxen.drunk.circe._`:

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

This code can be use to parse a response like:


```json
{
  "__typename": "Droid"
  "name": "R2D2"
  ....
}
```

> Take into account the client automatically adds the '__typename' field to every selector, so it's not required to be added in the queries.


## Extensions

There are several extension for GraphQL, this client supports:

* [Apollo cache control](https://github.com/apollographql/apollo-cache-control)
* [sangria-slowlog](http://sangria-graphql.org/learn/#profiling-graphql-query-execution)


To get the information of the extensions you can:

```scala     
  val cursor: GraphQLCursor = client.query[HeroQuery](query)
  val extensions: Future[GraphQLExtensions] = cursor.extensions
  val metrics: Future[Option[GraphQLMetricsExtension]] = extensions.map(_.metrics)
  val cacheControl: Future[Option[GraphQLCacheControlExtension]] = extensions.map(_.cacheControl)
```

## Contributing

If you have a question, or hit a problem, feel free to ask in the [issues](https://github.com/jarlakxen/drunk/issues)!

Or, if you encounter a bug, something is unclear in the code or documentation, don’t hesitate and open an issue.
