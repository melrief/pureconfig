package pureconfig.module.http4s.blaze.server.internal

import java.net.{ InetAddress, InetSocketAddress }

import org.http4s.server.{ KeyStoreBits, defaults }
import pureconfig.ConfigReader
import pureconfig.module.http4s.blaze.server.BlazeServerBuilderConfig

private[server] object Derivation {

  def blazeServerBuilderConfig(
    implicit
    ISA: ConfigReader[InetSocketAddress],
    KS: ConfigReader[KeyStoreBits]): ConfigReader[BlazeServerBuilderConfig] = {
    pureconfig.generic.semiauto.deriveReader[BlazeServerBuilderConfig]
  }

  final case class Socket(
      addr: InetAddress = defaults.SocketAddress.getAddress,
      port: Int = defaults.SocketAddress.getPort)

  def socket(
    implicit
    IA: ConfigReader[InetAddress]): ConfigReader[Socket] = {
    pureconfig.generic.semiauto.deriveReader[Socket]
  }
}
