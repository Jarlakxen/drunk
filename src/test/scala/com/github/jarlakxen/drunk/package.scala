package com.github.jarlakxen

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.testkit._

import org.scalatest.BeforeAndAfterAll

package object drunk {

  trait TestHttpServer extends BeforeAndAfterAll {
    this: Spec =>

    implicit val system: ActorSystem = ActorSystem("drunk-test")
    implicit def executor = system.dispatcher
    implicit val materializer = ActorMaterializer()

    private def temporaryServerAddress(interface: String = "127.0.0.1"): InetSocketAddress = {
      val serverSocket = ServerSocketChannel.open()
      try {
        serverSocket.socket.bind(new InetSocketAddress(interface, 0))
        val port = serverSocket.socket.getLocalPort
        new InetSocketAddress(interface, port)
      } finally serverSocket.close()
    }

    private def temporaryServerHostnameAndPort(interface: String = "127.0.0.1"): (String, Int) = {
      val socketAddress = temporaryServerAddress(interface)
      (socketAddress.getHostName, socketAddress.getPort)
    }

    val (host, port) = temporaryServerHostnameAndPort()

    override protected def beforeAll(): Unit =
      Http().bindAndHandle(serverRoutes, host, port).futureValue

    override protected def afterAll(): Unit =
      TestKit.shutdownActorSystem(system)

    def serverRoutes: Route
  }

}
