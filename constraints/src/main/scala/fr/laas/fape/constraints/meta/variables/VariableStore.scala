package fr.laas.fape.constraints.meta.variables

import scala.collection.mutable

class VariableStore {

  private var nextID : Int = 0

  val varsByRef = mutable.Map[Any, Variable]()

  def getNextVariableId() : Int = { nextID += 1; nextID-1 }

  def getVariable(ref: Option[Any] = None) : Variable = new Variable(getNextVariableId(), ref)

  def getBooleanVariable(ref: Option[Any] = None) : BooleanVariable =
    new BooleanVariable(getNextVariableId(), ref)

  def getVariableForRef(ref: Any) = {
    if(varsByRef.contains(ref))
      varsByRef(ref)
    else
      varsByRef.getOrElseUpdate(ref, new Variable(getNextVariableId(), Some(ref)))
  }

  def hasVariableForRef(ref: Any) = varsByRef.contains(ref)

  def setVariableForRef(ref: Any, variable: Variable): Unit = {
    assert(!hasVariableForRef(ref))
    varsByRef.put(ref, variable)
  }

}
