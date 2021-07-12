package pureconfig.module.http4s.blaze

import java.net.{ InetAddress, InetSocketAddress }

import cats.syntax.all._
import org.http4s.server.KeyStoreBits
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

package object server {

  implicit val configInetAddress: ConfigReader[InetAddress] =
    ConfigReader.fromString { str =>
      Either
        .catchNonFatal(InetAddress.getByName(str))
        .fold(
          err => Left(CannotConvert(str, "InetAddress", err.getMessage)),
          addr => Right(addr))
    }

  implicit def configInetSocketAddress(
    implicit
    IA: ConfigReader[InetAddress]): ConfigReader[InetSocketAddress] = {
    internal.Derivation.socket(IA).emap { socket =>
      Either
        .catchNonFatal(new InetSocketAddress(socket.addr, socket.port))
        .fold(
          err =>
            Left(
              CannotConvert(
                socket.toString,
                "InetSocketAddress",
                err.getMessage)),
          socket => Right(socket))
    }
  }

  implicit val configReaderKeyStoreBits: ConfigReader[KeyStoreBits] = {
    import pureconfig.generic.auto._
    pureconfig.generic.semiauto.deriveReader[KeyStoreBits]
  }

}
