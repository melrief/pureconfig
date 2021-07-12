package pureconfig.module.http4s.blaze.client.internal

import org.http4s.client.blaze.ParserMode
import org.http4s.headers.`User-Agent`
import pureconfig.ConfigReader
import pureconfig.module.http4s.blaze.client.BlazeClientBuilderConfig

private[client] object Derivation {

  def blazeClientBuilderConfig(
    implicit
    PM: ConfigReader[ParserMode],
    UA: ConfigReader[`User-Agent`]): ConfigReader[BlazeClientBuilderConfig] = {
    pureconfig.generic.semiauto.deriveReader[BlazeClientBuilderConfig]
  }
}
