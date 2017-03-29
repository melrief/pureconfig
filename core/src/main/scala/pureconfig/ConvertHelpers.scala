package pureconfig

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }
import scala.util.control.NonFatal

import com.typesafe.config._
import pureconfig.error._

/**
 * Useful helpers for building `ConfigConvert` instances and dealing with results.
 */
trait ConvertHelpers {

  def combineResults[A, B, C](first: Either[ConfigReaderFailures, A], second: Either[ConfigReaderFailures, B])(f: (A, B) => C): Either[ConfigReaderFailures, C] =
    (first, second) match {
      case (Right(a), Right(b)) => Right(f(a, b))
      case (Left(aFailures), Left(bFailures)) => Left(aFailures ++ bFailures)
      case (_, l: Left[_, _]) => l.asInstanceOf[Left[ConfigReaderFailures, Nothing]]
      case (l: Left[_, _], _) => l.asInstanceOf[Left[ConfigReaderFailures, Nothing]]
    }

  def fail[A](failure: ConfigReaderFailure): Either[ConfigReaderFailures, A] = Left(ConfigReaderFailures(failure))

  def failWithThrowable[A](throwable: Throwable): Option[ConfigValueLocation] => Either[ConfigReaderFailures, A] = location => fail[A](ThrowableFailure(throwable, location, None))

  private[pureconfig] def improveFailures[Z](result: Either[ConfigReaderFailures, Z], keyStr: String, location: Option[ConfigValueLocation]): Either[ConfigReaderFailures, Z] =
    result.left.map {
      case ConfigReaderFailures(head, tail) =>
        val headImproved = head.withImprovedContext(keyStr, location)
        val tailImproved = tail.map(_.withImprovedContext(keyStr, location))
        ConfigReaderFailures(headImproved, tailImproved)
    }

  private[pureconfig] def eitherToResult[T](either: Either[ConfigReaderFailure, T]): Either[ConfigReaderFailures, T] =
    either match {
      case r: Right[_, _] => r.asInstanceOf[Either[ConfigReaderFailures, T]]
      case Left(failure) => Left(ConfigReaderFailures(failure))
    }

  private[pureconfig] def tryToEither[T](t: Try[T]): Option[ConfigValueLocation] => Either[ConfigReaderFailure, T] = t match {
    case Success(v) => _ => Right(v)
    case Failure(e) => location => Left(ThrowableFailure(e, location, None))
  }

  private[pureconfig] def stringToTryConvert[T](fromF: String => Try[T]): ConfigValue => Either[ConfigReaderFailures, T] =
    stringToEitherConvert[T](string => location => tryToEither(fromF(string))(location))

  private[pureconfig] def stringToEitherConvert[T](fromF: String => Option[ConfigValueLocation] => Either[ConfigReaderFailure, T]): ConfigValue => Either[ConfigReaderFailures, T] =
    config => {
      // Because we can't trust Typesafe Config not to throw, we wrap the
      // evaluation into a `try-catch` to prevent an unintentional exception from escaping.
      try {
        val string = config.valueType match {
          case ConfigValueType.STRING => config.unwrapped.toString
          case _ => config.render(ConfigRenderOptions.concise)
        }
        eitherToResult(fromF(string)(ConfigValueLocation(config)))
      } catch {
        case NonFatal(t) => failWithThrowable(t)(ConfigValueLocation(config))
      }
    }

  private[pureconfig] def ensureNonEmpty[T](implicit ct: ClassTag[T]): String => Option[ConfigValueLocation] => Either[ConfigReaderFailure, String] = {
    case "" => location => Left(EmptyStringFound(ct.toString(), location, None))
    case x => _ => Right(x)
  }

  def catchReadError[T](f: String => T)(implicit ct: ClassTag[T]): String => Option[ConfigValueLocation] => Either[CannotConvert, T] =
    string => location =>
      try Right(f(string)) catch {
        case NonFatal(ex) => Left(CannotConvert(string, ct.toString(), ex.toString, location, None))
      }

  /**
   * Convert a `String => Try` into a  `String => Option[ConfigValueLocation] => Either` such that after application
   * - `Success(t)` becomes `_ => Right(t)`
   * - `Failure(e)` becomes `location => Left(CannotConvert(value, type, e.getMessage, location)`
   */
  def tryF[T](f: String => Try[T])(implicit ct: ClassTag[T]): String => Option[ConfigValueLocation] => Either[CannotConvert, T] =
    string => location =>
      f(string) match {
        case Success(t) => Right(t)
        case Failure(e) => Left(CannotConvert(string, ct.runtimeClass.getName, e.getLocalizedMessage, location, None))
      }

  /**
   * Convert a `String => Option` into a `String => Option[ConfigValueLocation] => Either` such that after application
   * - `Some(t)` becomes `_ => Right(t)`
   * - `None` becomes `location => Left(CannotConvert(value, type, "", location)`
   */
  def optF[T](f: String => Option[T])(implicit ct: ClassTag[T]): String => Option[ConfigValueLocation] => Either[CannotConvert, T] =
    string => location =>
      f(string) match {
        case Some(t) => Right(t)
        case None => Left(CannotConvert(string, ct.runtimeClass.getName, "", location, None))
      }
}

object ConvertHelpers extends ConvertHelpers
