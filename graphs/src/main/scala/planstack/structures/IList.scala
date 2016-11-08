package planstack.structures

import java.util

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

class IList[T](protected[structures] val l : List[T]) extends java.lang.Iterable[T] {

  def this() = this(Nil)
  def this(l : java.lang.Iterable[T]) = this(l.asScala.toList)

  class IListIterator[X](private var l : List[X]) extends util.Iterator[X] {
    val it = l.iterator
    override def hasNext: Boolean = it.hasNext
    override def next(): X = it.next()
    override def remove(): Unit = ???
  }

  def asScala = l

  def size(): Int = l.size

  def withoutAll(p1: util.Collection[_]): IList[T] =
    if(p1.isEmpty)
      this
    else
      new IList(l.filter(e => !p1.contains(e)))

  def onlyWithAll(p1: util.Collection[_]): IList[T] =
    new IList(l.filter(e => p1.contains(e)))

  def without(p1: scala.Any): IList[T] =
    new IList(l.filter(e => !(p1 == e)))

  def filter(f: T => Boolean) : IList[T] =
    new IList[T](l.filter(f))

  def map[T2](f: T => T2) : IList[T2] =
    new IList[T2](l.map(f))

  def contains(p1: scala.Any): Boolean =
    l.contains(p1)

  override def iterator(): util.Iterator[T] =
    new IListIterator[T](l)

  def withAll(p1: util.Collection[_ <: T]): IList[T] =
    if(p1.isEmpty)
      this
    else
      new IList(l ++ p1)

  def containsAll(p1: util.Collection[_]): Boolean = p1.forall(l.contains(_))

  def isEmpty: Boolean = l.isEmpty

  def `with`(p1: T): IList[T] =
    new IList(p1 :: l)

  def get(i: Int): T = l(i)

  /** Returns the first element of the list */
  def head = l.head

  def stream : java.util.stream.Stream[T] = l.asJavaCollection.stream()

  override def toString = l.toString()
}
