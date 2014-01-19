package planstack.constraints.bindings

import org.scalatest.Suite


class ConstraintManagerSuite extends Suite {

  def testBasics() {
    val cm = new ConstraintManager[String, String]()
    cm.addVar("A", List("a","b","c"))

    cm.addVar("B", List("a","b","c"))

    assert(cm.isPossibleValue("A", "c"))
    assert(!cm.isPossibleValue("A", "e"))
    assert(cm.isPossibleValue("B", "c"))
    assert(!cm.isPossibleValue("B", "e"))

    cm.cn.print()

    cm.bindVarToVar("A", "B")
    cm.assignValueToVar("A", "c")
    cm.propagate()

    cm.cn.print()

    assert(cm.isPossibleValue("A", "c"))
    assert(!cm.isPossibleValue("A", "a"))
    assert(cm.isPossibleValue("B", "c"))
    assert(!cm.isPossibleValue("B", "b"))

    cm.addVar("C", List("a","b","c"))
    cm.addVar("D", List("a","b","c"))

    cm.diffVarToVar("C", "B")
    cm.propagate()

    assert(cm.isPossibleValue("C", "a"))
    assert(cm.isPossibleValue("C", "b"))
    assert(!cm.isPossibleValue("C", "c"))

    cm.cn.print()
  }

}
