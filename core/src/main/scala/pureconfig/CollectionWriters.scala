package pureconfig

import scala.collection.JavaConverters._
import scala.language.higherKinds

import com.typesafe.config._

/**
 * A trait signaling that a `ConfigWriter` can write missing (undefined) values.
 *
 * `ConfigWriter`s always produce a valid `ConfigValue` with their `to` method. This trait adds an extra `toOpt`
 * method that parent writers can use in order to decide whether or not they should write a value using this writer.
 */
trait WritesMissingKeys[A] { this: ConfigWriter[A] =>
  def toOpt(a: A): Option[ConfigValue]
}

/**
 * Trait containing `ConfigWriter` instances for collection types.
 */
trait CollectionWriters {

  implicit def optionWriter[A](implicit conv: Derivation[ConfigWriter[A]]): ConfigWriter[Option[A]] =
    new ConfigWriter[Option[A]] with WritesMissingKeys[Option[A]] {
      override def to(t: Option[A]): ConfigValue = t match {
        case Some(v) => conv.value.to(v)
        case None => ConfigValueFactory.fromAnyRef(null)
      }

      def toOpt(t: Option[A]): Option[ConfigValue] = t.map(conv.value.to)
    }

  implicit def traversableWriter[A, F[A] <: TraversableOnce[A]](
    implicit
    configConvert: Derivation[ConfigWriter[A]]) = new ConfigWriter[F[A]] {

    override def to(ts: F[A]): ConfigValue = {
      ConfigValueFactory.fromIterable(ts.toList.map(configConvert.value.to).asJava)
    }
  }

  implicit def mapWriter[A](implicit configConvert: Derivation[ConfigWriter[A]]) = new ConfigWriter[Map[String, A]] {
    override def to(keyVals: Map[String, A]): ConfigValue = {
      ConfigValueFactory.fromMap(keyVals.mapValues(configConvert.value.to).toMap.asJava)
    }
  }

  implicit def arrayWriter[A](implicit writer: Derivation[ConfigWriter[A]]) = new ConfigWriter[Array[A]] {
    override def to(a: Array[A]): ConfigValue =
      ConfigValueFactory.fromIterable(a.toList.map(writer.value.to).asJava)
  }
}

object CollectionWriters extends CollectionWriters
