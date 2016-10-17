package fr.laas.fape.constraints

import org.scalatest.FunSuite
import planstack.UniquelyIdentified

import scala.collection.mutable
import scala.language.implicitConversions

object Ref {
  val map = mutable.Map[String,Int]()
  private var next = 0

  def get(name:String) = {
    if(!map.contains(name)) {
      map += ((name, next))
      next += 1
    }
    map(name)
  }

  implicit def toRef(name:String) : Ref = new Ref(name)
}

object IRef {
  implicit def toIRef(i:Int) : IRef = new IRef(i)
}

class IRef(val id: Int) extends UniquelyIdentified {
  override def toString = id.toString
}

class Ref(val name:String) extends UniquelyIdentified {

  override val id = Ref.get(name)

  override def equals(o: Any) : Boolean = o match {
    case r:Ref => r.name == name
    case i:Int => i.toString == name
    case s:String => s == name
  }

  override def hashCode : Int = name.hashCode

}

class MixedConstraintsSuite extends FunSuite {

  /*
  test("isPropagated") {
    val csp = Factory.getMetaWithoutControllability[Ref,Ref,Ref]

    val iDomain = new java.util.LinkedList[Integer]
    for(i <- 1 until 5)
      iDomain.add(i)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMaxDelay("A","B","d")

    assert(csp.varsToConstraints.nonEmpty)

    iDomain.removeFirst()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.nonEmpty)

    iDomain.remove()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.nonEmpty)

    iDomain.remove()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.isEmpty)

    assert(csp.stn.isConsistent())
    csp.stn.enforceMinDelay("A","B",2)
    assert(csp.stn.isConsistent())
    csp.stn.enforceMinDelay("A","B",4)
    assert(csp.stn.isConsistent())
    csp.stn.enforceMinDelay("A","B",5)
    assert(!csp.stn.isConsistent())
  }

  test("is Propagated Right Away On Singleton Domain (Max)") {
    val csp = new MetaCSP[Ref,Ref,Ref]()

    val iDomain = new java.util.LinkedList[Integer]
    iDomain.add(4)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMaxDelay("A","B","d")
    // should be propagated already
    assert(csp.varsToConstraints.isEmpty)

    csp.stn.enforceMinDelay("A","B",4)
    assert(csp.stn.isConsistent())
    csp.stn.enforceMinDelay("A","B",5)
    assert(!csp.stn.isConsistent())
  }

  test("is Propagated Right Away On Singleton Domain (Min)") {
    val csp = new MetaCSP[Ref,Ref,Ref]()

    val iDomain = new java.util.LinkedList[Integer]
    iDomain.add(4)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMinDelay("A","B","d")
    // should be propagated already
    assert(csp.varsToConstraints.isEmpty)

    csp.stn.enforceMaxDelay("A","B",4)
    assert(csp.stn.isConsistent())
    csp.stn.enforceMaxDelay("A","B",3)
    assert(!csp.stn.isConsistent())
  }
*/

}
