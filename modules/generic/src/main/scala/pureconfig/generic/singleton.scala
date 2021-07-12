package pureconfig.generic

import scala.reflect.runtime.universe._

import shapeless._
import shapeless.syntax.typeable._

import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigWriter}

object singleton {
  final implicit def singletonReader[T: Typeable, W >: T](implicit
      ev: Widen.Aux[T, W],
      tt: TypeTag[T],
      reader: ConfigReader[W]
  ): ConfigReader[T] =
    reader.emap(w =>
      w.narrowTo[T].toRight(CannotConvert(w.toString, tt.tpe.toString, "Unable to narrow to singleton type."))
    )

  final implicit def singletonWriter[T, W >: T](implicit
      ev: Widen.Aux[T, W],
      writer: ConfigWriter[W]
  ): ConfigWriter[T] =
    writer.contramap(identity)
}
