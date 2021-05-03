package pureconfig
package generic
package derivation

import scala.compiletime.{constValue, erasedValue, summonFrom, summonInline}
import scala.deriving.Mirror

import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.error.InvalidCoproductOption

trait CoproductConfigReader[A] extends ConfigReader[A]
object CoproductConfigReader {
  inline def derived[A](using m: Mirror.SumOf[A]): CoproductConfigReader[A] = {
    val hint = summonInline[CoproductHint[A]]
    new CoproductConfigReader[A] {
      def from(cur: ConfigCursor): ConfigReader.Result[A] =
        for {
          result <- {
            val options = labelsFor[m.MirroredElemLabels]
            val optionReaders =
              options
                .zip(deriveForSubtypes[m.MirroredElemTypes, A])
                .toMap

            for {
              action <- hint.from(cur, options)
              result <-
                action match {
                  case action: CoproductHint.Use => handleAction[A](action, optionReaders)
                  case action: CoproductHint.Attempt => handleAction[A](action, optionReaders)
                }
            } yield result
          }
        } yield result
    }
  }

  inline def handleAction[A](action: CoproductHint.Use, optionReaders: Map[String, ConfigReader[A]]) =
    optionReaders.get(action.option) match {
      case Some(reader) => reader.from(action.cursor)
      case None =>
        ConfigReader.Result.fail[A](
          action.cursor.failureFor(InvalidCoproductOption(action.option))
        )
    }

  type AttemptResult[A] = Either[Vector[(String, ConfigReaderFailures)], A]

  inline def handleAction[A](action: CoproductHint.Attempt, optionReaders: Map[String, ConfigReader[A]]) =
    action.options
      .foldLeft[AttemptResult[A]](Left(Vector.empty)) { (curr, option) =>
        curr.left.flatMap { currentFailures =>
          optionReaders.get(option) match {
            case Some(value) => value.from(action.cursor).left.map(f => currentFailures :+ (option -> f))
            case None =>
              Left(
                currentFailures :+
                  (option -> ConfigReaderFailures(action.cursor.failureFor(InvalidCoproductOption(option))))
              )
          }
        }
      }
      .left
      .map(action.combineFailures)

  inline def deriveForSubtypes[T <: Tuple, A]: List[ConfigReader[A]] =
    inline erasedValue[T] match {
      case _: (h *: t) => deriveForSubtype[h, A] :: deriveForSubtypes[t, A]
      case _: EmptyTuple => Nil
    }

  inline def deriveForSubtype[A0, A]: ConfigReader[A] =
    summonFrom { case given Mirror.Of[A0] =>
      ConfigReader.derived[A0].map(_.expandType[A])
    }
}
