package com.github.jarlakxen.drunk

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest._
import org.scalatest.concurrent.{Futures, ScalaFutures}
import org.scalatest.time._
import org.slf4j.LoggerFactory

trait Spec
    extends FlatSpec
    with Matchers
    with OptionValues
    with Inside
    with Retries
    with TryValues
    with Inspectors
    with TypeCheckedTripleEquals
    with Futures
    with ScalaFutures
    with RecoverMethods { self =>

  val log = LoggerFactory.getLogger(this.getClass)

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(5, Millis))

}
