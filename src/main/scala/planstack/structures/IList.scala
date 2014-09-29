package planstack.structures

import java.util

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class IList[T](protected val l : List[T]) extends java.lang.Iterable[T] {

  def this() = this(Nil)
  def this(l : IList[T]) = this(l.l)
  def this(l : java.lang.Iterable[T]) = this(l.asScala.toList)

  class IListIterator[X](private var l : List[X]) extends util.Iterator[X] {
    override def hasNext: Boolean = l.nonEmpty

    override def next(): X = {
      val item = l.head
      l = l.tail
      item
    }

    override def remove(): Unit = ???
  }


  def size(): Int = l.size

  def withoutAll(p1: util.Collection[_]): IList[T] =
    new IList(l.filter(e => !p1.contains(e)))

  def onlyWithAll(p1: util.Collection[_]): IList[T] =
    new IList(l.filter(e => p1.contains(e)))

  def without(p1: scala.Any): IList[T] =
    new IList(l.filter(e => !(p1 == e)))

  def contains(p1: scala.Any): Boolean =
    l.contains(p1)

  override def iterator(): util.Iterator[T] =
    new IListIterator[T](l)

  def withAll(p1: util.Collection[_ <: T]): IList[T] =
    new IList(l ++ p1)

  def containsAll(p1: util.Collection[_]): Boolean = p1.forall(l.contains(_))

  def isEmpty: Boolean = l.isEmpty

  def `with`(p1: T): IList[T] =
    new IList(p1 :: l)

  def get(i: Int): T = l(i)

  override def toString = l.toString()
}
