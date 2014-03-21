package planstack.constraints.bindings

class CSPVar(val id:Int, val dom:Set[Int]) {
  override def toString = "%d={%s}".format(id, dom.toString().replace("Set",""))

  def remove(v:Int) : CSPVar = new CSPVar(id, dom.filter(_ != v))
  def setVal(v:Int) : CSPVar = new CSPVar(id, Set[Int](v))

  override def hashCode() = id.hashCode()
  override def equals(obj:Any) = obj match {
    case v:CSPVar => id == v.id
    case _ => false
  }
}

class Constraint

abstract class UnaryConstraint(val x:Int) extends Constraint {
  def isSatisfying(xVal:Int) : Boolean
}

class EqValue(x:Int, val value:Int) extends UnaryConstraint(x){
  def isSatisfying(xVal: Int): Boolean = xVal == value
}

class DiffValue(x:Int, val value:Int) extends UnaryConstraint(x){
  def isSatisfying(xVal: Int): Boolean = xVal != value
}



abstract class BinaryConstraint(val x:Int, val y:Int) extends Constraint {
  def isSatisfying(xVal:Int, yVal:Int) : Boolean
}


class Equal(x:Int, y:Int) extends BinaryConstraint(x, y) {

  def isSatisfying(xVal: Int, yVal: Int): Boolean = xVal == yVal
}

class Different(x:Int, y:Int) extends BinaryConstraint(x, y) {

  def isSatisfying(xVal: Int, yVal: Int): Boolean = xVal != yVal
}


class ExtendedConstraint(x:Int, y:Int, pairs:Set[Pair[Int,Int]])extends BinaryConstraint(x,y) {
  def isSatisfying(xVal: Int, yVal: Int): Boolean =
    !pairs.forall(p => p._1 != xVal || p._2 != yVal)
}

class GenExtConstraint[VarT,ValT](val x:VarT, val y:VarT, val pairs:Set[Pair[ValT,ValT]])



object Main extends App {

  val cn = new ConstraintManager[String, String]

  cn.addVar("Rb", Set("Rb"))
  cn.addVar("Rb.left", Set("G1"))
  cn.addVar("Rb2", Set("Rb2"))
  cn.addVar("Rb2.left", Set("G3"))



  cn.addVar("G1", Set("G1"))
  cn.addVar("G2", Set("G2"))
  cn.addVar("G3", Set("G3"))
  cn.addVar("G4", Set("G4"))

  cn.addVar("r", Set("Rb", "Rb2"))
  cn.addVar("g", Set("G1", "G2", "G3"))

  cn.addVar("r.left", Set("Rb.left", "Rb2.left"))

  val ext = new GenExtConstraint("r", "r.left", Set(("Rb","Rb.left"), ("Rb2","Rb2.left")))
  cn.addExtensionConstraint(ext)

  cn.print()





  //cn.dotPrinter().print2Dot("/home/abitmonn/tmp/g.dot")
}
