package pureconfig.module.http4s.blaze.server

import java.net.InetSocketAddress

import cats.effect.{ ContextShift, IO, Timer }
import com.typesafe.config.ConfigFactory
import org.http4s.server.blaze.BlazeServerBuilder
import pureconfig.BaseSuite
import pureconfig.syntax._

import scala.concurrent.ExecutionContext.global

class Http4sBlazeServerTest extends BaseSuite {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  "reading a BlazeServerBuilderConfig" should "allow creating a BlazeServerBuilder" in {
    val conf = ConfigFactory.parseString(s"""
         |{
         |  socket: {
         |    addr: "127.0.0.123"
         |    port: 1234
         |  }
         |  banner: ["a", "b"]
         |  response-header-timeout: "1 s"
         |  idle-timeout: "2 s"
         |  max-header-length: 8
         |}
         |""".stripMargin)

    val Right(res) = conf.to[BlazeServerBuilderConfig]
    val serverBuilder = res.toBuilder[IO]()

    serverBuilder.resource
      .use { server =>
        server.address.getAddress.getHostAddress should ===("127.0.0.123")
        server.address.getPort should ===(1234)
        IO.unit
      }
      .unsafeRunSync()
  }

  // can fail in CI if not in perfect isolation
  ignore should "Default be the same as unmodified BlazeServerBuilder" in {

    implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
    implicit val timer: Timer[IO] = IO.timer(global)

    var actualAddress: InetSocketAddress = null
    var actualIsSecure: Boolean = true
    val actual = BlazeServerBuilderConfig().toBuilder[IO]().resource
    actual
      .use { server =>
        actualAddress = server.address
        actualIsSecure = server.isSecure
        IO.unit
      }
      .unsafeRunSync()

    val expected = BlazeServerBuilder[IO].resource
    expected
      .use { server =>
        server.address should ===(actualAddress)
        server.isSecure should ===(actualIsSecure)
        IO.unit
      }
      .unsafeRunSync()
  }

}
