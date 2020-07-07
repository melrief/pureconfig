package pureconfig.generic

import pureconfig._
import shapeless._
import scala.reflect.ClassTag

/**
 * An object that provides methods for deriving `ConfigReader` and `ConfigWriter` instances on demand for value
 * classes, tuples, case classes and sealed traits.
 */
object semiauto {
  final def deriveReader[A](implicit reader: Lazy[DerivedConfigReader[A]]): ConfigReader[A] = reader.value
  final def deriveWriter[A](implicit writer: Lazy[DerivedConfigWriter[A]]): ConfigWriter[A] = writer.value

  final def deriveConvert[A](implicit reader: Lazy[DerivedConfigReader[A]], writer: Lazy[DerivedConfigWriter[A]]): ConfigConvert[A] =
    ConfigConvert(reader.value, writer.value)

  /**
   * Derive a `ConfigReader` for a sealed family of case objects where each type is encoded as the kebab-case
   * representation of the type name.
   */
  final def deriveEnumerationReader[A: ClassTag](
    implicit
    readerBuilder: Lazy[EnumerationConfigReaderBuilder[A]]): ConfigReader[A] =
    deriveEnumerationReader(ConfigFieldMapping(PascalCase, KebabCase))

  /**
   * Derive a `ConfigWriter` for a sealed family of case objects where each type is encoded as the kebab-case
   * representation of the type name.
   */
  final def deriveEnumerationWriter[A](
    implicit
    writerBuilder: Lazy[EnumerationConfigWriterBuilder[A]]): ConfigWriter[A] =
    deriveEnumerationWriter(ConfigFieldMapping(PascalCase, KebabCase))

  /**
   * Derive a `ConfigConvert` for a sealed family of case objects where each type is encoded as the kebab-case
   * representation of the type name.
   */
  final def deriveEnumerationConvert[A: ClassTag](
    implicit
    readerBuilder: Lazy[EnumerationConfigReaderBuilder[A]],
    writerBuilder: Lazy[EnumerationConfigWriterBuilder[A]]): ConfigConvert[A] =
    deriveEnumerationConvert(ConfigFieldMapping(PascalCase, KebabCase))

  /**
   * Derive a `ConfigReader` for a sealed family of case objects where each type is encoded with the `transformName`
   * function applied to the type name.
   */
  final def deriveEnumerationReader[A](transformName: String => String)(
    implicit
    readerBuilder: Lazy[EnumerationConfigReaderBuilder[A]],
    ct: ClassTag[A]): ConfigReader[A] = readerBuilder.value.build(transformName, Set.empty, ct.runtimeClass.getSimpleName)

  /**
   * Derive a `ConfigWriter` for a sealed family of case objects where each type is encoded with the `transformName`
   * function applied to the type name.
   */
  final def deriveEnumerationWriter[A](transformName: String => String)(
    implicit
    writerBuilder: Lazy[EnumerationConfigWriterBuilder[A]]): ConfigWriter[A] = writerBuilder.value.build(transformName)

  /**
   * Derive a `ConfigConvert` for a sealed family of case objects where each type is encoded with the `transformName`
   * function applied to the type name.
   */
  final def deriveEnumerationConvert[A](transformName: String => String)(
    implicit
    readerBuilder: Lazy[EnumerationConfigReaderBuilder[A]],
    writerBuilder: Lazy[EnumerationConfigWriterBuilder[A]],
    ct: ClassTag[A]): ConfigConvert[A] =
    ConfigConvert(
      readerBuilder.value.build(transformName, Set.empty, ct.runtimeClass.getSimpleName),
      writerBuilder.value.build(transformName))
}
