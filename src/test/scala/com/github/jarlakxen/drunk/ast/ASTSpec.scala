package com.github.jarlakxen.drunk.ast

import com.github.jarlakxen.drunk.Spec
import sangria.macros._

class ASTSpec extends Spec {

  "AST tools" should "add typename in simple query" in {

    val doc =
      graphql"""
        query HeroAndFriends {
          hero {
            id
            name
          }
        }
      """

    val expected =
      graphql"""
        query HeroAndFriends {
          hero {
            __typename
            id
            name
          }
        }
      """

    addTypename(doc).renderPretty shouldBe expected.renderPretty
  }

  it should "add typename to nested selectors" in {

    val doc =
      graphql"""
        query HeroAndFriends {
          hero {
            id
            name
            friends(size: 10) {
              name
            }
          }
        }
      """

    val expected =
      graphql"""
        query HeroAndFriends {
          hero {
            __typename
            id
            name
            friends(size: 10) {
              __typename
              name
            }
          }
        }
      """

    addTypename(doc).renderPretty shouldBe expected.renderPretty
  }

  it should "add typename to mutiple queries" in {

    val doc =
      graphql"""
        query {
          human(id: "123454") {
            name
            homePlanet
            friends {
              name
            }
          }

          human(id: "577522") {
            name
            homePlanet
            friends {
              name
            }
          }
        }
      """

    val expected =
      graphql"""
        query {
          human(id: "123454") {
            __typename
            name
            homePlanet
            friends {
              __typename
              name
            }
          }

          human(id: "577522") {
            __typename
            name
            homePlanet
            friends {
              __typename
              name
            }
          }
        }
      """

    addTypename(doc).renderPretty shouldBe expected.renderPretty
  }
  
    it should "add typename to fragments" in {

    val doc =
      graphql"""
        query {
          human(id: "123454") {
            ...HumanFragment
          }
        }
    

        fragment HumanFragment on Human {
          name
          homePlanet
          friends {
            name
          }
        }
      """

    val expected =
      graphql"""
        query {
          human(id: "123454") {
            ...HumanFragment
          }
        }
    

        fragment HumanFragment on Human {
          __typename
          name
          homePlanet
          friends {
            __typename
            name
          }
        }
      """
      
    addTypename(doc).renderPretty shouldBe expected.renderPretty
  }

}