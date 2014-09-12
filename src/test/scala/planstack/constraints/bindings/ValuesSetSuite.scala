package planstack.constraints.bindings



import collection.JavaConversions._
import org.scalatest.{FunSuite, FunSpec}

class ValuesSetSuite extends FunSuite {
  def S(vals : Integer*) : java.util.Set[Integer] = asJavaSet(vals.toSet)

  val ext1 = new ExtensionConstraint(false)

  ext1.addValues(List[Integer](11, 12, 13))
  ext1.addValues(List[Integer](21, 22, 23))
  ext1.addValues(List[Integer](31, 32, 33))

  //  println(ext.valuesUnderRestriction(0, c))

  test("a") {
    val c = Map[Integer, java.util.Set[Integer]]((1, Set[Integer](12)), (2, Set[Integer](13, 23)))
    assert(ext1.valuesUnderRestriction(0, c) == S(11))
  }

  test("b") {
    val c = Map[Integer, java.util.Set[Integer]]((2, Set[Integer](13, 23)))
    assert(ext1.valuesUnderRestriction(0, c) == S(11, 21))
  }

  test("c") {
    val c = Map[Integer, java.util.Set[Integer]]((1, Set[Integer](12, 22, 32)), (2, Set[Integer](13, 33)))
    assert(ext1.valuesUnderRestriction(0, c) == S(11, 31))
  }

  test("d") {
    val c = Map[Integer, java.util.Set[Integer]]((0, Set[Integer](11)), (2, Set[Integer](13, 33)))
    assert(ext1.valuesUnderRestriction(1, c) == S(12))
  }

  val ext2 = new ExtensionConstraint(false)

  ext2.addValues(List[Integer](11, 12, 13))
  ext2.addValues(List[Integer](11, 22, 23))
  ext2.addValues(List[Integer](11, 22, 33))

  test("a2") {
    val c = Map[Integer, java.util.Set[Integer]]((0, Set[Integer](11)))
    assert(ext2.valuesUnderRestriction(1, c) == S(12, 22))
    assert(ext2.valuesUnderRestriction(2, c) == S(13, 23, 33))
  }

  test("b2") {
    val c = Map[Integer, java.util.Set[Integer]]((2, Set[Integer](13, 23)))
    assert(ext2.valuesUnderRestriction(0, c) == S(11))
    assert(ext2.valuesUnderRestriction(1, c) == S(12, 22))
  }

  test("c2") {
    val c = Map[Integer, java.util.Set[Integer]]((1, Set[Integer](22)), (2, Set[Integer](13, 33)))
    assert(ext2.valuesUnderRestriction(0, c) == S(11))
  }

  test("d2") {
    val c = Map[Integer, java.util.Set[Integer]]((0, Set[Integer](11)), (1, Set[Integer](22)))
    assert(ext2.valuesUnderRestriction(2, c) == S(23, 33))
  }
}
