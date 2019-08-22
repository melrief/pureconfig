package pureconfig.module

import _root_.scalaz.{ ==>>, DList, Dequeue, Heap, ICons, IList, INil, ISet, Maybe, NonEmptyList, Order }
import _root_.scalaz.std.list._
import _root_.scalaz.syntax.foldable._
import pureconfig._
import pureconfig.error.FailureReason

/**
 * `ConfigReader` and `ConfigWriter` instances for `scalaz` data structures.
 */
package object scalaz {

  case object EmptyIListFound extends FailureReason {
    def description: String =
      "Empty scalaz.IList found when trying to convert to scalaz.NonEmptyList."
  }

  implicit def dListConvert[T](implicit cc: ConfigConvert[List[T]]): ConfigConvert[DList[T]] =
    cc.xmap(l => DList.fromList(l), _.toList)

  implicit def dequeueConvert[T](implicit cc: ConfigConvert[List[T]]): ConfigConvert[Dequeue[T]] =
    cc.xmap(l => Dequeue.fromFoldable(l), _.toList)

  implicit def heapConvert[T: Order](implicit cc: ConfigConvert[List[T]]): ConfigConvert[Heap[T]] =
    cc.xmap(l => Heap.fromData(l), _.toList)

  implicit def iListConvert[T](implicit cc: ConfigConvert[List[T]]): ConfigConvert[IList[T]] =
    cc.xmap(IList.fromList, _.toList)

  implicit def iSetConvert[T: Order](implicit cc: ConfigConvert[List[T]]): ConfigConvert[ISet[T]] =
    cc.xmap(l => ISet.fromList(l), _.toList)

  implicit def mapConvert[A: Order, B](implicit cc: ConfigConvert[Map[A, B]]): ConfigConvert[A ==>> B] =
    cc.xmap(m => ==>>.fromList(m.toList), _.toList.toMap)

  implicit def nonEmptyListReader[T](implicit cr: ConfigReader[IList[T]]): ConfigReader[NonEmptyList[T]] =
    cr.emap {
      case ICons(h, t) => Right(NonEmptyList.nel(h, t))
      case INil() => Left(EmptyIListFound)
    }

  implicit def nonEmptyListWriter[T](implicit cw: ConfigWriter[IList[T]]): ConfigWriter[NonEmptyList[T]] =
    cw.contramap(_.list)

  implicit def maybeConvert[T](implicit convert: ConfigConvert[Option[T]]): ConfigConvert[Maybe[T]] =
    convert.xmap(Maybe.fromOption, _.toOption)
}
