package fr.laas.fape.constraints.meta.typing

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.types.dynamics.{BaseDynamicType, ComposedDynamicType, DynTypedVariable, DynamicType}
import fr.laas.fape.constraints.meta.types.events.NewInstance
import fr.laas.fape.constraints.meta.types.statics.{BaseType, Type}
import org.scalatest.{BeforeAndAfter, FunSuite}

class DynamicTypeTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = null
  var t: DynamicType[String] = null
  var v1 : DynTypedVariable[String] = null

  before {
    csp = new CSP
    class DynType extends DynamicType[String] {
      override def isStatic: Boolean = false
      override def defaultStatic: Type[String] = BaseType("base", List("a", "b", "c"))
      override def subTypes = Nil
    }
    t = new DynType
    v1 = new DynTypedVariable(t)
  }

  test("dynamic type with valid addition") {
    csp.post(v1 =!= t.static.instanceToInt("b"))
    csp.propagate()

    assert(v1.dom.contains("a"))
    assert(!v1.dom.contains("b"))
    println("Before new instance: "+v1.dom)

    csp.addEvent(NewInstance(t, "d", 10))
    csp.propagate()

    println("After new instance: "+v1.dom)

    assert(v1.dom.contains("d"))
    assert(v1.domain.contains(10))
  }

  test("dynamic type with invalid addition") {
    csp.post(v1 === t.static.instanceToInt("a"))
    csp.propagate()

    assert(v1.dom.contains("a"))
    assert(!v1.dom.contains("b"))
    println("Before new instance: "+v1.dom)

    csp.addEvent(NewInstance(t, "d", 10))
    csp.propagate()

    println("After new instance: "+v1.dom)

    assert(!v1.dom.contains("d"))
    assert(!v1.domain.contains(10))
  }

  test("Hierarchical dynamic type") {
    val staticBaseT = new BaseType("static", List(("a", 0), ("b", 1), ("c", 2)))
    val dynBaseT = new BaseDynamicType("dyn-base", List(("A", 10), ("B", 11), ("C", 12)))
    val T = new ComposedDynamicType(List(staticBaseT, dynBaseT))

    val v = new DynTypedVariable(T)

    csp.post(v =!= T.static.instanceToInt("b"))
    csp.propagate()

    assert(v.dom.contains("a"))
    assert(v.dom.contains("A"))
    assert(!v.dom.contains("b"))
    println("Before new instance: "+v.dom)

    csp.addEvent(NewInstance(dynBaseT, "d", 3))
    csp.propagate()

    println("After new instance: "+v.dom)

    assert(v.dom.contains("d"))
    assert(v.domain.contains(3))

    assert(v.dom.contains("a"))
    assert(v.dom.contains("A"))
    assert(!v.dom.contains("b"))
  }

}
