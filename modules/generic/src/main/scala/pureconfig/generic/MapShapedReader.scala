package pureconfig.generic

import pureconfig._
import pureconfig.error.KeyNotFound
import pureconfig.generic.ProductHint.UseOrDefault
import shapeless._
import shapeless.labelled.{ FieldType, field }

/**
 * A specialized reader for generic representations that reads values in the shape of a config object, and is capable
 * of handling default values.
 *
 * @tparam Wrapped the original type for which `Repr` is a generic sub-representation
 * @tparam Repr the generic representation
 * @tparam DefaultRepr the default representation of the original type
 */
private[generic] trait MapShapedReader[Wrapped, Repr, DefaultRepr] {
  def from(cur: ConfigObjectCursor, default: DefaultRepr, usedFields: Set[String]): ConfigReader.Result[Repr]
}

object MapShapedReader {

  implicit def labelledHNilReader[Wrapped](
    implicit
    hint: ProductHint[Wrapped]): MapShapedReader[Wrapped, HNil, HNil] =
    new MapShapedReader[Wrapped, HNil, HNil] {
      def from(cur: ConfigObjectCursor, default: HNil, usedFields: Set[String]): ConfigReader.Result[HNil] =
        hint.bottom(cur, usedFields).fold[ConfigReader.Result[HNil]](Right(HNil))(Left.apply)
    }

  final implicit def labelledHConsReader[Wrapped, K <: Symbol, H, T <: HList, D <: HList](
    implicit
    key: Witness.Aux[K],
    hConfigReader: Derivation[Lazy[ConfigReader[H]]],
    tConfigReader: Lazy[MapShapedReader[Wrapped, T, D]],
    hint: ProductHint[Wrapped]): MapShapedReader[Wrapped, FieldType[K, H] :: T, Option[H] :: D] =
    new MapShapedReader[Wrapped, FieldType[K, H] :: T, Option[H] :: D] {
      def from(cur: ConfigObjectCursor, default: Option[H] :: D, usedFields: Set[String]): ConfigReader.Result[FieldType[K, H] :: T] = {
        val fieldName = key.value.name
        val fieldAction = hint.from(cur, fieldName)
        lazy val reader = hConfigReader.value.value
        val headResult = (fieldAction, default.head) match {
          case (UseOrDefault(cursor, _), Some(defaultValue)) if cursor.isUndefined =>
            Right(defaultValue)
          case (action, _) if !action.cursor.isUndefined || reader.isInstanceOf[ReadsMissingKeys] =>
            reader.from(action.cursor)
          case _ =>
            cur.failed[H](KeyNotFound.forKeys(fieldAction.field, cur.keys))
        }
        val tailResult = tConfigReader.value.from(cur, default.tail, usedFields + fieldAction.field)
        ConfigReader.Result.zipWith(headResult, tailResult)((head, tail) => field[K](head) :: tail)
      }
    }
}
