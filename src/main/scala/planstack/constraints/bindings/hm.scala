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







object Main extends App {

  val cn = new ConstraintNetwork


  //cn.dotPrinter().print2Dot("/home/abitmonn/tmp/g.dot")
}
