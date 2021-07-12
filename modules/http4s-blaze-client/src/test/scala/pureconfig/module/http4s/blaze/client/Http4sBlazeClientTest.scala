package pureconfig.module.http4s.blaze.client

import java.util.concurrent.TimeUnit

import cats.effect.{ ContextShift, IO }
import com.typesafe.config.ConfigFactory
import org.http4s.client.blaze.BlazeClientBuilder
import pureconfig.BaseSuite
import pureconfig.syntax._

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.Duration

class Http4sBlazeClientTest extends BaseSuite {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)

  "reading a BlazeClientBuilderConfig" should "allow creating a BlazeClientBuilder" in {
    val conf =
      ConfigFactory.parseString(s"""
           |{ 
           |  response-header-timeout: "1 s"
           |  idle-timeout: "2 s"
           |  request-timeout: "3 s"
           |  max-total-connections: 4
           |  max-wait-queue-limit: 5
           |  check-endpoint-authentication: true
           |  max-response-line-size: 7
           |  max-header-length: 8
           |  max-chunk-size: 9
           |  chunk-buffer-max-size: 10
           |  buffer-size: 11
           |}
           |""".stripMargin)

    val Right(res) = conf.to[BlazeClientBuilderConfig]
    val clientBuilder = res.toBuilder[IO](global)

    clientBuilder.responseHeaderTimeout should === {
      Duration.create(1, TimeUnit.SECONDS)
    }
    clientBuilder.idleTimeout should === {
      Duration.create(2, TimeUnit.SECONDS)
    }
    clientBuilder.requestTimeout should === {
      Duration.create(3, TimeUnit.SECONDS)
    }
    clientBuilder.maxTotalConnections should === {
      4
    }
    clientBuilder.maxWaitQueueLimit should === {
      5
    }
    clientBuilder.checkEndpointIdentification should === {
      true
    }
    clientBuilder.maxResponseLineSize should === {
      7
    }
    clientBuilder.maxHeaderLength should === {
      8
    }
    clientBuilder.maxChunkSize should === {
      9
    }
    clientBuilder.chunkBufferMaxSize should === {
      10
    }
    clientBuilder.bufferSize should === {
      11
    }

    clientBuilder.resource
      .use { _ =>
        IO.unit
      }
      .unsafeRunSync()
  }

  "Default" should "be the same as unmodified BlazeClientBuilder" in {

    val actual = BlazeClientBuilderConfig().toBuilder[IO](global)
    val expected = BlazeClientBuilder[IO](global)

    actual.responseHeaderTimeout should ===(expected.responseHeaderTimeout)
    actual.idleTimeout should ===(expected.idleTimeout)
    actual.requestTimeout should ===(expected.requestTimeout)
    actual.connectTimeout should ===(expected.connectTimeout)
    actual.userAgent should ===(expected.userAgent)
    actual.maxTotalConnections should ===(expected.maxTotalConnections)
    actual.maxWaitQueueLimit should ===(expected.maxWaitQueueLimit)
    actual.checkEndpointIdentification should ===(
      expected.checkEndpointIdentification)
    actual.maxResponseLineSize should ===(expected.maxResponseLineSize)
    actual.maxHeaderLength should ===(expected.maxHeaderLength)
    actual.maxChunkSize should ===(expected.maxChunkSize)
    actual.chunkBufferMaxSize should ===(expected.chunkBufferMaxSize)
    actual.parserMode should ===(expected.parserMode)
    actual.bufferSize should ===(expected.bufferSize)
  }

}
