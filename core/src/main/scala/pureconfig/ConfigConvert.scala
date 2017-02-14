/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
/**
 * @author Mario Pastorelli
 */
package pureconfig

import com.typesafe.config._
import shapeless._
import shapeless.labelled._
import scala.collection.JavaConverters._
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }
import java.net.URL
import java.nio.file.{ Path, Paths }
import java.time._
import java.util.UUID
import javax.security.auth.kerberos.KerberosPrincipal

import scala.concurrent.duration.{ Duration, FiniteDuration }

import pureconfig.ConfigConvert.{ fromNonEmptyString, fromString, nonEmptyStringConvert, stringConvert }
import pureconfig.error._
import scala.collection.mutable.Builder
import scala.util.control.NonFatal

/**
 * Trait for conversion between `T` and `ConfigValue`.
 */
trait ConfigConvert[T] {
  /**
   * Convert the given configuration into an instance of `T` if possible.
   *
   * @param config The configuration from which load the config
   * @return `Success` of `T` if the conversion is possible, `Failure` with the problem if the
   *         conversion is not
   */
  def from(config: ConfigValue): Try[T]

  /**
   * Converts a type `T` to a `ConfigValue`.
   *
   * @param t The instance of `T` to convert
   * @return The `ConfigValue` obtained from the `T` instance
   */
  def to(t: T): ConfigValue
}

/**
 * The default behavior of ConfigConverts that are implicitly derived in PureConfig is to raise a
 * KeyNotFoundException when a required key is missing. Mixing in this trait to a ConfigConvert
 * allows customizing this behavior. When a key is missing, but the ConfigConvert of the given
 * type extends this trait, the `from` method of the ConfigConvert is called with null.
 */
trait AllowMissingKey { self: ConfigConvert[_] => }

object ConfigConvert extends LowPriorityConfigConvertImplicits {
  def apply[T](implicit conv: ConfigConvert[T]): ConfigConvert[T] = conv

  private def fromFConvert[T](fromF: String => Try[T]): ConfigValue => Try[T] =
    config => {
      // Because we can't trust `fromF` or Typesafe Config not to throw, we wrap the
      // evaluation in one more `Try` to prevent an unintentional exception from escaping.
      // `Try.flatMap(f)` captures any non-fatal exceptions thrown by `f`.
      Try(config.valueType match {
        case ConfigValueType.STRING => config.unwrapped.toString
        case _ => config.render(ConfigRenderOptions.concise)
      }).flatMap(fromF)
    }

  def fromString[T](fromF: String => Try[T]): ConfigConvert[T] = new ConfigConvert[T] {
    override def from(config: ConfigValue): Try[T] = fromFConvert(fromF)(config)
    override def to(t: T): ConfigValue = ConfigValueFactory.fromAnyRef(t)
  }

  def fromNonEmptyString[T](fromF: String => Try[T])(implicit ct: ClassTag[T]): ConfigConvert[T] = {
    fromString(ensureNonEmpty(ct)(_).flatMap(fromF))
  }

  private def ensureNonEmpty[T](implicit ct: ClassTag[T]): String => Try[String] = {
    case "" => Failure(new IllegalArgumentException(s"Cannot read a $ct from an empty string."))
    case x => Success(x)
  }

  def stringConvert[T](fromF: String => Try[T], toF: T => String): ConfigConvert[T] = new ConfigConvert[T] {
    override def from(config: ConfigValue): Try[T] = fromFConvert(fromF)(config)
    override def to(t: T): ConfigValue = ConfigValueFactory.fromAnyRef(toF(t))
  }

  def nonEmptyStringConvert[T](fromF: String => Try[T], toF: T => String)(implicit ct: ClassTag[T]): ConfigConvert[T] = {
    stringConvert(ensureNonEmpty(ct)(_).flatMap(fromF), toF)
  }

  private[pureconfig] trait WrappedConfigConvert[Wrapped, SubRepr] extends ConfigConvert[SubRepr]

  private[pureconfig] trait WrappedDefaultValueConfigConvert[Wrapped, SubRepr <: HList, DefaultRepr <: HList] extends WrappedConfigConvert[Wrapped, SubRepr] {
    final def from(config: ConfigValue): Try[SubRepr] =
      Failure(
        new UnsupportedOperationException("Cannot call 'from' on a WrappedDefaultValueConfigConvert."))
    def fromWithDefault(config: ConfigValue, default: DefaultRepr): Try[SubRepr] = config match {
      case co: ConfigObject =>
        fromConfigObject(co, default)
      case other =>
        Failure(WrongTypeException(config.valueType().toString))
    }
    def fromConfigObject(co: ConfigObject, default: DefaultRepr): Try[SubRepr]
    def to(v: SubRepr): ConfigValue
  }

  implicit def hNilConfigConvert[Wrapped](
    implicit
    hint: ProductHint[Wrapped]): WrappedDefaultValueConfigConvert[Wrapped, HNil, HNil] = new WrappedDefaultValueConfigConvert[Wrapped, HNil, HNil] {

    override def fromConfigObject(config: ConfigObject, default: HNil): Try[HNil] = {
      if (!hint.allowUnknownKeys && !config.isEmpty) Failure(UnknownKeyException(config.keySet.iterator.next))
      else Success(HNil)
    }

    override def to(t: HNil): ConfigValue = ConfigFactory.parseMap(Map().asJava).root()
  }

  private[pureconfig] def improveFailure[Z](result: Try[Z], keyStr: String): Try[Z] =
    result recoverWith {
      case CannotConvertNullException => Failure(KeyNotFoundException(keyStr))
      case KeyNotFoundException(suffix) => Failure(KeyNotFoundException(keyStr + "." + suffix))
      case WrongTypeException(typ) => Failure(WrongTypeForKeyException(typ, keyStr))
      case WrongTypeForKeyException(typ, suffix) => Failure(WrongTypeForKeyException(typ, keyStr + "." + suffix))
    }

  implicit def hConsConfigConvert[Wrapped, K <: Symbol, V, T <: HList, U <: HList](
    implicit
    key: Witness.Aux[K],
    vFieldConvert: Lazy[ConfigConvert[V]],
    tConfigConvert: Lazy[WrappedDefaultValueConfigConvert[Wrapped, T, U]],
    hint: ProductHint[Wrapped]): WrappedDefaultValueConfigConvert[Wrapped, FieldType[K, V] :: T, Option[V] :: U] = new WrappedDefaultValueConfigConvert[Wrapped, FieldType[K, V] :: T, Option[V] :: U] {

    override def fromConfigObject(co: ConfigObject, default: Option[V] :: U): Try[FieldType[K, V] :: T] = {
      val keyStr = hint.configKey(key.value.toString().tail)
      for {
        v <- improveFailure[V](
          (co.get(keyStr), vFieldConvert.value) match {
            case (null, converter: AllowMissingKey) =>
              converter.from(co.get(keyStr))
            case (null, _) =>
              val defaultValue = if (hint.useDefaultArgs) default.head else None
              defaultValue.fold[Try[V]](Failure(CannotConvertNullException))(Success(_))
            case (value, converter) =>
              converter.from(value)
          },
          keyStr)

        // for performance reasons only, we shouldn't clone the config object unless necessary
        tailCo = if (hint.allowUnknownKeys) co else co.withoutKey(keyStr)

        tail <- tConfigConvert.value.fromWithDefault(tailCo, default.tail)
      } yield field[K](v) :: tail
    }

    override def to(t: FieldType[K, V] :: T): ConfigValue = {
      val keyStr = hint.configKey(key.value.toString().tail)
      val rem = tConfigConvert.value.to(t.tail)
      // TODO check that all keys are unique
      vFieldConvert.value match {
        case f: OptionConfigConvert[_] =>
          f.toOption(t.head) match {
            case Some(v) =>
              rem.asInstanceOf[ConfigObject].withValue(keyStr, v)
            case None =>
              rem
          }
        case f =>
          val fieldEntry = f.to(t.head)
          rem.asInstanceOf[ConfigObject].withValue(keyStr, fieldEntry)
      }
    }
  }

  case class NoValidCoproductChoiceFound(config: ConfigValue)
    extends RuntimeException(s"No valid coproduct type choice found for configuration $config.")

  implicit def cNilConfigConvert[Wrapped]: WrappedConfigConvert[Wrapped, CNil] = new WrappedConfigConvert[Wrapped, CNil] {
    override def from(config: ConfigValue): Try[CNil] =
      Failure(NoValidCoproductChoiceFound(config))

    override def to(t: CNil): ConfigValue = ConfigFactory.parseMap(Map().asJava).root()
  }

  implicit def coproductConfigConvert[Wrapped, Name <: Symbol, V, T <: Coproduct](
    implicit
    coproductHint: CoproductHint[Wrapped],
    vName: Witness.Aux[Name],
    vFieldConvert: Lazy[ConfigConvert[V]],
    tConfigConvert: Lazy[WrappedConfigConvert[Wrapped, T]]): WrappedConfigConvert[Wrapped, FieldType[Name, V] :+: T] =
    new WrappedConfigConvert[Wrapped, FieldType[Name, V] :+: T] {

      override def from(config: ConfigValue): Try[FieldType[Name, V] :+: T] =
        coproductHint.from(config, vName.value.name) match {
          case Success(Some(hintConfig)) =>
            vFieldConvert.value.from(hintConfig) match {
              case Failure(_) if coproductHint.tryNextOnFail(vName.value.name) =>
                tConfigConvert.value.from(config).map(s => Inr(s))

              case vTry => vTry.map(v => Inl(field[Name](v)))
            }

          case Success(None) => tConfigConvert.value.from(config).map(s => Inr(s))
          case Failure(ex) => Failure(ex)
        }

      override def to(t: FieldType[Name, V] :+: T): ConfigValue = t match {
        case Inl(l) =>
          // Writing a coproduct to a config can fail. Is it worth it to make `to` return a `Try`?
          coproductHint.to(vFieldConvert.value.to(l), vName.value.name).get

        case Inr(r) =>
          tConfigConvert.value.to(r)
      }
    }

  // For Option[T] we use a special config converter
  implicit def deriveOption[T](implicit conv: Lazy[ConfigConvert[T]]) = new OptionConfigConvert[T]

  class OptionConfigConvert[T](implicit conv: Lazy[ConfigConvert[T]]) extends ConfigConvert[Option[T]] with AllowMissingKey {
    override def from(config: ConfigValue): Try[Option[T]] = {
      if (config == null || config.unwrapped() == null)
        Success(None)
      else
        conv.value.from(config).map(Some(_))
    }

    override def to(t: Option[T]): ConfigValue = t match {
      case Some(v) => conv.value.to(v)
      case None => ConfigValueFactory.fromMap(Map().asJava)
    }

    def toOption(t: Option[T]): Option[ConfigValue] = t.map(conv.value.to)
  }

  // traversable of types with an instance of ConfigConvert
  implicit def deriveTraversable[T, F[T] <: TraversableOnce[T]](
    implicit
    configConvert: Lazy[ConfigConvert[T]],
    cbf: CanBuildFrom[F[T], T, F[T]]) = new ConfigConvert[F[T]] {

    override def from(config: ConfigValue): Try[F[T]] = {
      config match {
        case co: ConfigList =>
          traverseTry(co.asScala, cbf())(configConvert.value.from)
        case o: ConfigObject =>
          for {
            indexedEntries <- traverseTry(o.asScala, new collection.mutable.ListBuffer[(Int, ConfigValue)]) {
              case (keyString, value) => Try(keyString.toInt -> value).recoverWith(reportBadIndex(keyString))
            }
            sortedValues = indexedEntries.sortBy(_._1).map(_._2)
            result <- traverseTry(sortedValues, cbf())(configConvert.value.from)
          } yield result
        case other =>
          Failure(WrongTypeException(other.valueType().toString))
      }
    }

    // Akin to scalaz's `traverseU` but limited to operating on Try instead of all monads.
    private def traverseTry[A, B, C](i: TraversableOnce[A], builder: Builder[B, C])(f: A => Try[B]): Try[C] = {
      i.foldLeft(Try(builder)) {
        case (builderMaybe, a) => builderMaybe.flatMap { builder =>
          f(a).map(builder += _)
        }
      }.map(_.result)
    }

    private def reportBadIndex(keyString: String): PartialFunction[Throwable, Failure[Nothing]] = {
      case NonFatal(e) =>
        val message = s"Cannot interpet '$keyString' as a numeric index. Tried to read the object as an indexed sequence. " +
          "Expecting syntax like (a.0=0, a.1=1, ...) when loading a sequence called 'a'"
        Failure(new IllegalArgumentException(message))
    }

    override def to(ts: F[T]): ConfigValue = {
      ConfigValueFactory.fromIterable(ts.toList.map(configConvert.value.to).asJava)
    }
  }

  implicit def deriveMap[T](implicit configConvert: Lazy[ConfigConvert[T]]) = new ConfigConvert[Map[String, T]] {

    override def from(config: ConfigValue): Try[Map[String, T]] = {
      config match {
        case co: ConfigObject =>
          val keysFound = co.keySet().asScala.toList

          keysFound.foldLeft(Try(Map.empty[String, T])) {
            case (f @ Failure(_), _) => f
            case (Success(acc), key) =>
              for {
                rawValue <- Try(co.get(key))
                value <- configConvert.value.from(rawValue)
              } yield acc + (key -> value)
          }
        case other =>
          Failure(WrongTypeException(other.valueType().toString))
      }
    }

    override def to(keyVals: Map[String, T]): ConfigValue = {
      ConfigValueFactory.fromMap(keyVals.mapValues(configConvert.value.to).asJava)
    }
  }

  // used for products
  implicit def deriveProductInstance[F, Repr <: HList, DefaultRepr <: HList](
    implicit
    gen: LabelledGeneric.Aux[F, Repr],
    default: Default.AsOptions.Aux[F, DefaultRepr],
    cc: Lazy[WrappedDefaultValueConfigConvert[F, Repr, DefaultRepr]]): ConfigConvert[F] = new ConfigConvert[F] {

    override def from(config: ConfigValue): Try[F] = {
      cc.value.fromWithDefault(config, default()).map(gen.from)
    }

    override def to(t: F): ConfigValue = {
      cc.value.to(gen.to(t))
    }
  }

  // used for coproducts
  implicit def deriveCoproductInstance[F, Repr <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[F, Repr],
    cc: Lazy[WrappedConfigConvert[F, Repr]]): ConfigConvert[F] = new ConfigConvert[F] {
    override def from(config: ConfigValue): Try[F] = {
      cc.value.from(config).map(gen.from)
    }

    override def to(t: F): ConfigValue = {
      cc.value.to(gen.to(t))
    }
  }
}

/**
 * Implicit [[ConfigConvert]] instances defined such that they can be overriden by library consumer via a locally defined implementation.
 */
trait LowPriorityConfigConvertImplicits {
  implicit val durationConfigConvert: ConfigConvert[Duration] = {
    nonEmptyStringConvert(
      s => DurationConvert.fromString(s, implicitly[ClassTag[Duration]]),
      DurationConvert.fromDuration)
  }

  implicit val finiteDurationConfigConvert: ConfigConvert[FiniteDuration] = {
    val fromString: String => Try[FiniteDuration] = { (s: String) =>
      DurationConvert.fromString(s, implicitly[ClassTag[FiniteDuration]])
        .flatMap {
          case d: FiniteDuration => Success(d)
          case _ => Failure(new IllegalArgumentException(s"Couldn't parse '$s' into a FiniteDuration because it's infinite."))
        }
    }
    nonEmptyStringConvert(fromString, DurationConvert.fromDuration)
  }

  implicit val instantConfigConvert: ConfigConvert[Instant] =
    nonEmptyStringConvert[Instant](s => Try(Instant.parse(s)), _.toString)

  implicit val zoneOffsetConfigConvert: ConfigConvert[ZoneOffset] =
    nonEmptyStringConvert[ZoneOffset](s => Try(ZoneOffset.of(s)), _.toString)

  implicit val zoneIdConfigConvert: ConfigConvert[ZoneId] =
    nonEmptyStringConvert[ZoneId](s => Try(ZoneId.of(s)), _.toString)

  implicit val periodConfigConvert: ConfigConvert[Period] =
    nonEmptyStringConvert[Period](s => Try(Period.parse(s)), _.toString)

  implicit val yearConfigConvert: ConfigConvert[Year] =
    nonEmptyStringConvert[Year](s => Try(Year.parse(s)), _.toString)

  implicit val readString = fromString[String](Success(_))
  implicit val readBoolean = fromNonEmptyString[Boolean](s => Try(s.toBoolean))
  implicit val readDouble = fromNonEmptyString[Double]({
    case v if v.last == '%' => Try(v.dropRight(1).toDouble / 100d)
    case v => Try(v.toDouble)
  })
  implicit val readFloat = fromNonEmptyString[Float]({
    case v if v.last == '%' => Try(v.dropRight(1).toFloat / 100f)
    case v => Try(v.toFloat)
  })
  implicit val readInt = fromNonEmptyString[Int](s => Try(s.toInt))
  implicit val readLong = fromNonEmptyString[Long](s => Try(s.toLong))
  implicit val readShort = fromNonEmptyString[Short](s => Try(s.toShort))
  implicit val readURL = stringConvert[URL](s => Try(new URL(s)), _.toString)
  implicit val readUUID = stringConvert[UUID](s => Try(UUID.fromString(s)), _.toString)
  implicit val readPath = stringConvert[Path](s => Try(Paths.get(s)), _.toString)
  implicit val readKerberosPrincipal = stringConvert[KerberosPrincipal](s => Try(new KerberosPrincipal(s)), _.toString)

  implicit val readConfig: ConfigConvert[Config] = new ConfigConvert[Config] {
    override def from(config: ConfigValue): Try[Config] = config match {
      case co: ConfigObject => Success(co.toConfig)
      case other => Failure(WrongTypeException(other.valueType().toString))
    }
    override def to(t: Config): ConfigValue = t.root()
  }

  implicit val readConfigObject: ConfigConvert[ConfigObject] = new ConfigConvert[ConfigObject] {
    override def from(config: ConfigValue): Try[ConfigObject] = config match {
      case c: ConfigObject => Success(c)
      case other => Failure(WrongTypeException(other.valueType().toString))
    }
    override def to(t: ConfigObject): ConfigValue = t
  }

  implicit val readConfigValue: ConfigConvert[ConfigValue] = new ConfigConvert[ConfigValue] {
    override def from(config: ConfigValue): Try[ConfigValue] = Success(config)
    override def to(t: ConfigValue): ConfigValue = t
  }
  implicit val readConfigList: ConfigConvert[ConfigList] = new ConfigConvert[ConfigList] {
    override def from(config: ConfigValue): Try[ConfigList] = config match {
      case c: ConfigList => Success(c)
      case other => Failure(WrongTypeException(other.valueType().toString))
    }
    override def to(t: ConfigList): ConfigValue = t
  }
}
