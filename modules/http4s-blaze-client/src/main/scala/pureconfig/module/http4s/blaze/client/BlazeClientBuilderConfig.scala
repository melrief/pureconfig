package pureconfig.module.http4s.blaze.client

import cats.effect.ConcurrentEffect
import javax.net.ssl.SSLContext
import org.http4s.BuildInfo
import org.http4s.client.blaze.{ BlazeClientBuilder, ParserMode }
import org.http4s.client.defaults
import org.http4s.headers.{ AgentProduct, `User-Agent` }
import pureconfig.ConfigReader

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.higherKinds

final case class BlazeClientBuilderConfig(
    responseHeaderTimeout: Duration = Duration.Inf,
    idleTimeout: Duration = 1.minute,
    requestTimeout: Duration = defaults.RequestTimeout,
    connectTimeout: Duration = defaults.ConnectTimeout,
    userAgent: Option[`User-Agent`] = Some(
      `User-Agent`(AgentProduct("http4s-blaze", Some(BuildInfo.version)))),
    maxTotalConnections: Int = 10,
    maxWaitQueueLimit: Int = 256,
    checkEndpointIdentification: Boolean = true,
    maxResponseLineSize: Int = 4096,
    maxHeaderLength: Int = 40960,
    maxChunkSize: Int = Int.MaxValue,
    chunkBufferMaxSize: Int = 1024 * 1024,
    parserMode: ParserMode = ParserMode.Strict,
    bufferSize: Int = 8192) {

  private implicit class BuilderOps[A](val c: A) {
    def withOption[O](o: Option[O])(f: A => O => A): A = o match {
      case Some(value) => f(c)(value)
      case None => c
    }
  }

  def toBuilder[F[_]: ConcurrentEffect](
    ec: ExecutionContext,
    ssl: Option[SSLContext] = None): BlazeClientBuilder[F] = {

    BlazeClientBuilder(ec, ssl)
      .withResponseHeaderTimeout(responseHeaderTimeout)
      .withIdleTimeout(idleTimeout)
      .withRequestTimeout(requestTimeout)
      .withConnectTimeout(connectTimeout)
      .withOption(userAgent)(_.withUserAgent)
      .withMaxTotalConnections(maxTotalConnections)
      .withMaxWaitQueueLimit(maxWaitQueueLimit)
      .withCheckEndpointAuthentication(checkEndpointIdentification)
      .withMaxResponseLineSize(maxResponseLineSize)
      .withMaxHeaderLength(maxHeaderLength)
      .withMaxChunkSize(maxChunkSize)
      .withChunkBufferMaxSize(chunkBufferMaxSize)
      .withParserMode(parserMode)
      .withBufferSize(bufferSize)
  }
}

object BlazeClientBuilderConfig {

  implicit def configReaderBlazeClientBuilderConfig(
    implicit
    PM: ConfigReader[ParserMode],
    UA: ConfigReader[`User-Agent`]): ConfigReader[BlazeClientBuilderConfig] = {
    internal.Derivation.blazeClientBuilderConfig(PM, UA)
  }

}
