package pureconfig.module

import scala.util.Try

import org.apache.spark.sql.types.{DataType, StructType}

import pureconfig.{ConfigConvert, ConfigReader, ConfigWriter}

/** `ConfigConvert` instances for Spark data structures.
  */
package object spark {
  implicit val dataTypeConvert: ConfigConvert[DataType] =
    ConfigConvert.viaNonEmptyStringTry[DataType](s => Try(DataType.fromDDL(s)), _.sql)

  implicit val structTypeReader: ConfigReader[StructType] =
    ConfigReader.fromNonEmptyStringTry[StructType](s => Try(StructType.fromDDL(s)))

  implicit val structTypeWriter: ConfigWriter[StructType] =
    dataTypeConvert.contramap(identity)
}
