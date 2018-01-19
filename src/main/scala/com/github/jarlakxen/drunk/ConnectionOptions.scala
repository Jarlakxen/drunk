/*
 * Copyright 2018 Facundo Viale
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.jarlakxen.drunk

import java.net.InetSocketAddress

import scala.concurrent.duration._
import scala.util.Try

case class ConnectionOptions(connectionTimeout: FiniteDuration, proxy: Option[ConnectionOptions.Proxy]) {
  import ConnectionOptions._

  def connectionTimeout(ct: FiniteDuration): ConnectionOptions =
    this.copy(connectionTimeout = ct)

  def httpProxy(host: String, port: Int): ConnectionOptions =
    this.copy(proxy = Some(Proxy(host, port, ProxyType.Http)))

  def socksProxy(host: String, port: Int): ConnectionOptions =
    this.copy(proxy = Some(Proxy(host, port, ProxyType.Socks)))
}

object ConnectionOptions {

  case class Proxy(host: String, port: Int, proxyType: ProxyType) {
    def inetSocketAddress: InetSocketAddress =
      InetSocketAddress.createUnresolved(host, port)
  }

  sealed trait ProxyType

  object ProxyType {
    case object Http extends ProxyType
    case object Socks extends ProxyType
  }

  private val Empty: ConnectionOptions =
    ConnectionOptions(30.seconds, None)

  val Default: ConnectionOptions =
    Empty.copy(proxy = loadSystemProxy)

  def connectionTimeout(ct: FiniteDuration): ConnectionOptions =
    Default.connectionTimeout(ct)

  def httpProxy(host: String, port: Int): ConnectionOptions =
    Empty.httpProxy(host, port)

  def socksProxy(host: String, port: Int): ConnectionOptions =
    Empty.socksProxy(host, port)

  private def loadSystemProxy: Option[Proxy] = {
    def system(hostProp: String, portProp: String, make: (String, Int) => Proxy, defaultPort: Int) = {
      val host = Option(System.getProperty(hostProp))
      def port = Try(System.getProperty(portProp).toInt).getOrElse(defaultPort)
      host.map(make(_, port))
    }

    def proxy(t: ProxyType)(host: String, port: Int) = Proxy(host, port, t)

    import ProxyType._
    val socks = system("socksProxyHost", "socksProxyPort", proxy(Socks), 1080)
    val http = system("http.proxyHost", "http.proxyPort", proxy(Http), 80)

    socks orElse http
  }

}
