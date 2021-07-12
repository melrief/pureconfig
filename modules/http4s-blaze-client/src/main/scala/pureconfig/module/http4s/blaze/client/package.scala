package pureconfig.module.http4s.blaze

import org.http4s.client.blaze.ParserMode
import org.http4s.headers.`User-Agent`
import pureconfig.ConfigReader

package object client {

  implicit val configReaderParserMode: ConfigReader[ParserMode] = {
    import pureconfig.generic.auto._
    pureconfig.generic.semiauto.deriveReader[ParserMode]
  }

  implicit val configReaderUserAgent: ConfigReader[`User-Agent`] = {
    import pureconfig.generic.auto._
    pureconfig.generic.semiauto.deriveReader[`User-Agent`]
  }

}
