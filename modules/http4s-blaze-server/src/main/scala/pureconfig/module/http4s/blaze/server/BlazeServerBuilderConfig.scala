package pureconfig.module.http4s.blaze.server

import java.net.InetSocketAddress

import cats.effect.{ ConcurrentEffect, Timer }
import org.http4s.blaze.channel._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{ KeyStoreBits, defaults }
import pureconfig.ConfigReader

import scala.concurrent.duration.Duration
import scala.language.higherKinds

final case class BlazeServerBuilderConfig(
    socket: InetSocketAddress = defaults.SocketAddress,
    responseHeaderTimeout: Duration = defaults.ResponseTimeout,
    idleTimeout: Duration = defaults.IdleTimeout,
    nio2: Boolean = false,
    connectorPoolSize: Int = DefaultPoolSize,
    bufferSize: Int = 64 * 1024,
    enableWebSockets: Boolean = true,
    keyStoreBits: Option[KeyStoreBits] = None,
    http2: Boolean = false,
    maxRequestLineLength: Int = 4 * 1024,
    maxHeadersLength: Int = 40 * 1024,
    chunkBufferMaxSize: Int = 1024 * 1024,
    banner: List[String] = defaults.Banner) {

  private implicit class BuilderOps[A](val c: A) {
    def withOption[O](o: Option[O])(f: A => O => A): A = o match {
      case Some(value) => f(c)(value)
      case None => c
    }
  }

  def toBuilder[F[_]: ConcurrentEffect: Timer](): BlazeServerBuilder[F] = {
    BlazeServerBuilder[F]
      .bindSocketAddress(socket)
      .withResponseHeaderTimeout(responseHeaderTimeout)
      .withIdleTimeout(idleTimeout)
      .withNio2(nio2)
      .withConnectorPoolSize(connectorPoolSize)
      .withBufferSize(bufferSize)
      .withWebSockets(enableWebSockets)
      .withOption(keyStoreBits) { builder => value =>
        builder.withSSL(
          value.keyStore,
          value.keyManagerPassword,
          value.protocol,
          value.trustStore,
          value.clientAuth)
      }
      .enableHttp2(http2)
      .withMaxRequestLineLength(maxRequestLineLength)
      .withMaxHeadersLength(maxHeadersLength)
      .withChunkBufferMaxSize(chunkBufferMaxSize)
      .withBanner(banner)

  }
}

object BlazeServerBuilderConfig {

  implicit def configReaderBlazeServerBuilderConfig(
    implicit
    ISA: ConfigReader[InetSocketAddress],
    KS: ConfigReader[KeyStoreBits]): ConfigReader[BlazeServerBuilderConfig] = {
    internal.Derivation.blazeServerBuilderConfig(ISA, KS)
  }
}
