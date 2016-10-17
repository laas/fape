package fr.laas.fape.constraints.bindings

import org.scalatest.FunSuite

import scala.collection.JavaConversions._

class ValuesSetSuite extends FunSuite {
  def S(vals : Integer*) : java.util.Set[Integer] = setAsJavaSet(vals.toSet)

  val ext1 = new ExtensionConstraint("aaaa", false, 3)

  ext1.addValues(List[Integer](11, 12, 13))
  ext1.addValues(List[Integer](21, 22, 23))
  ext1.addValues(List[Integer](31, 32, 33))

  //  println(ext.restrictedDomains(0, c))

  test("a") {
    val c = Array(S(11,21,31), S(12), S(13, 23))
    assert(ext1.restrictedDomains(c)(0) == S(11))
  }

  test("b") {
    val c = Array(S(11,21,31), S(12,22,32), S(13, 23))
    assert(ext1.restrictedDomains(c)(0) == S(11, 21))
  }

  test("c") {
    val c = Array(S(11,21,31), S(12,22,32), S(13, 33))
    assert(ext1.restrictedDomains(c)(0) == S(11, 31))
  }

  test("d") {
    val c = Array(S(11), S(12,22,32), S(13, 33))
    assert(ext1.restrictedDomains(c)(1) == S(12))
  }

  val ext2 = new ExtensionConstraint("bbbb", false, 3)

  ext2.addValues(List[Integer](11, 12, 13))
  ext2.addValues(List[Integer](11, 22, 23))
  ext2.addValues(List[Integer](11, 22, 33))

  test("a2") {
    val c = Array(S(11), S(12,22,32), S(13,23,33))
    assert(ext2.restrictedDomains(c)(1) == S(12, 22))
    assert(ext2.restrictedDomains(c)(2) == S(13, 23, 33))
  }

  test("b2") {
    val c = Array(S(11,21,31), S(12,22,32), S(13,23))
    assert(ext2.restrictedDomains(c)(0) == S(11))
    assert(ext2.restrictedDomains(c)(1) == S(12, 22))
  }

  test("c2") {
    val c = Array(S(11,21,31), S(22), S(13,33))
    assert(ext2.restrictedDomains(c)(0) == S(11))
  }

  test("d2") {
    val c = Array(S(11), S(22), S(13,23,33))
    assert(ext2.restrictedDomains(c)(2) == S(23, 33))
  }
}
