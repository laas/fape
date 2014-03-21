package planstack.constraints.bindings

import scala.collection.mutable
import planstack.constraints._


/**
 * This class is wrapper for a ConstraintNetwork (that only deals with integers) to allow
 * manipulating constraints on any type.
 *
 * For instance for a map colouring problem, you could use this constraint manager with
 * VarT=Country and ValT=Color.
 *
 * This class uses Map data structures to perform the translation from an Object to an integer.
 * Therefore make sure that the type parameters have consistent hashCode() and equals() methods.
 *
 * @tparam VarT type of the variables of the CSP
 * @tparam ValT type of the values of the CSP
 */
class ConstraintManager[VarT, ValT] {

  val cn = new ConstraintNetwork()

  protected var nextVarId = 0
  protected var nextValId = 0


  /**
   * Maps from a value to its id
   */
  protected val valId = mutable.Map[ValT, Int]()
  /**
   * Maps from an id to the corresponding value
   */
  protected val idVal = mutable.ArrayBuffer[ValT]()

  /**
   * Maps from a variable to the corresponding variable
   */
  protected val varId = mutable.Map[VarT, Int]()
  /**
   * Maps from an id to the corresponding variable object
   */
  protected val idVar = mutable.ArrayBuffer[VarT]()

  /**
   * @param v
   * @return true if the value v is known of the CSP (was already added)
   */
  def containsVal(v:ValT) = valId.contains(v)

  /**
   * @param v
   * @return true if the variable v was already added to the CSP
   */
  def containsVar(v:VarT) = varId.contains(v)

  def declareVal(v:ValT) = {
    if(containsVal(v)) {
      false
    } else {
      valId += ((v, nextValId))
      idVal.append(v)
      assert(nextValId == valId(v) && idVal(nextValId) == v)
      nextValId += 1
      true
    }
  }

  def addVar(v:VarT, dom:Seq[ValT]) { addVar(v, dom.toSet) }

  def addVar(v:VarT, dom:Set[ValT])  {
    dom.foreach(declareVal(_))
    if(containsVar(v))
      throw new Exception("Var already exists: " + v)
    val cspVar = cn.newVar(dom.map(valId(_)))
    assert(cspVar.id == nextVarId)
    varId += ((v, cspVar.id))
    idVar += v
    nextVarId += 1
  }

  /**
   * Test if value is a possible assignment to variable
   * @param variable
   * @param value
   * @return True if the assignment is possible. False otherwise.
   */
  def isPossibleValue(variable:VarT, value:ValT) : Boolean = {
    assert(containsVar(variable))
    if(!containsVal(value))
      return false

    cn.node(varId(variable)).dom.contains(valId(value))
  }


  /**
   * Assign a value to a variable
   * @param variable
   * @param value
   */
  def assignValueToVar(variable:VarT, value:ValT) { cn.addConstraint(new EqValue(varId(variable), valId(value))) }

  /**
   * Unifies the two variables (i.e. they must have the same value)
   * @param v1
   * @param v2
   */
  def bindVarToVar(v1:VarT, v2:VarT) { cn.addConstraint(new Equal(varId(v1), varId(v2))) }


  /**
   * Stipulates that value is not acceptable for variable
   * @param variable
   * @param value
   */
  def diffVarToVal(variable:VarT, value:ValT) { cn.addConstraint(new DiffValue(varId(variable), valId(value))) }

  /**
   * Stipulates that those to variables can't have the same value
   * @param v1
   * @param v2
   */
  def diffVarToVar(v1:VarT, v2:VarT) { cn.addConstraint(new Different(varId(v1), varId(v2))) }

  def addExtensionConstraint(ext:GenExtConstraint[VarT,ValT]) {
    cn.addConstraint(new ExtendedConstraint(varId(ext.x), varId(ext.y), ext.pairs.map(p => (valId(p._1),valId(p._2)))))
  }

  def propagate() = cn.AC3()

  def varToString(v:CSPVar) = "%s = {%s}".format(idVar(v.id), v.dom.map(idVal(_)).mkString(", "))

  def print() { cn.cn.mVertices.map(cn.node(_)).foreach(variable => println(varToString(variable))) }
}
