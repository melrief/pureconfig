package pureconfig.module.scalaz

import org.scalactic.Equality

import scalaz.{ DList, Heap }

package object equality {

  implicit def dListEquality[A]: Equality[DList[A]] = new Equality[DList[A]] {
    override def areEqual(a: DList[A], b: Any): Boolean = b match {
      case l: DList[_] => implicitly[Equality[List[A]]].areEqual(a.toList, l.toList)
      case _ => false
    }
  }

  implicit def heapEquality[A]: Equality[Heap[A]] = new Equality[Heap[A]] {
    override def areEqual(a: Heap[A], b: Any): Boolean = b match {
      case l: Heap[_] => implicitly[Equality[List[A]]].areEqual(a.toList, l.toList)
      case _ => false
    }
  }
}
