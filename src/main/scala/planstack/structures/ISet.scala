package planstack.structures

import java.util

import scala.collection.JavaConverters._

class ISet[T](val s : Set[T]) extends java.lang.Iterable[T] {

  def this() = this(Set[T]())
  def this(l : java.lang.Iterable[T]) = this(l.asScala.toSet)

  class ISetIterator[X](private var s : Set[X]) extends java.util.Iterator[X] {
    override def hasNext: Boolean = s.nonEmpty

    override def next(): X = {
      val item = s.head
      s = s.tail
      item
    }

    override def remove(): Unit = ???
  }

  def asScala = s


  def size(): Int = s.size

  def head(): T = s.head

  def withoutAll(p1: util.Collection[T]): ISet[T] =
    new ISet(s.filter(e => !p1.contains(e)))

  def onlyWithAll(p1: util.Collection[_]): ISet[T] =
    new ISet(s.filter(e => p1.contains(e)))

  def without(p1: scala.Any): ISet[T] =
    new ISet(s.filter(e => !(p1 == e)))

  def filter(f: T => Boolean) : ISet[T] =
    new ISet[T](s.filter(f))

  def contains(p1: T): Boolean =
    s.contains(p1)

  override def iterator(): util.Iterator[T] =
    new ISetIterator[T](s)

  def withAll(p1: util.Collection[T]): ISet[T] =
    new ISet(s ++ p1.asScala)

  def containsAll(p1: util.Collection[T]): Boolean = p1.asScala.forall(item => s.contains(item))

  def isEmpty: Boolean = s.isEmpty

  def `with`(p1: T): ISet[T] =
    new ISet(s + p1)

  override def toString = s.toString()
}
