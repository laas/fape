package planstack.anml.model

import scala.collection.mutable
import planstack.anml.ANMLException
import scala.collection.mutable.ListBuffer
import planstack.anml.model.concrete.{ActRef, VarRef}


abstract class AbstractContext {

  def parentContext : Option[AbstractContext]
  protected val variables = mutable.Map[LVarRef, Pair[String, VarRef]]()

  protected val actions = mutable.Map[LActRef, ActRef]()

  /**
   * @param localName Name of the local variable to look up
   * @return a pair (type, globalName) of the local variable
   */
  protected def getDefinition(localName:LVarRef) : Pair[String, VarRef] = {
    if(variables.contains(localName)) {
      variables(localName)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unable to find local var: "+localName)
        case Some(parent) => parent.getDefinition(localName)
      }
    }
  }

  def contains(localName:LVarRef) = {
    try {
      getDefinition(localName)
      true
    } catch {
      case _:Exception => false
    }
  }

  def getType(localName:LVarRef) : String = getDefinition(localName)._1

  def getGlobalVar(localName:LVarRef) : VarRef = {
    val (tipe, globalName) = getDefinition(localName)
    if(globalName.isEmpty)
      throw new ANMLException("Variable %s has no global definition".format(localName))
    else
      globalName
  }

  def addVar(localName:LVarRef, typeName:String, globalName:VarRef) {
    assert(!variables.contains(localName))
    variables.put(localName, (typeName, globalName))
  }

  def getActionID(localID:LActRef) : ActRef = {
    if(actions.contains(localID)) {
      actions(localID)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unknown action local ID: "+localID)
        case Some(parent) => parent.getActionID(localID)
      }
    }
  }

  def addActionID(localID:LActRef, globalID:ActRef) {
    assert(!actions.contains(localID) || actions(localID).isEmpty)
    actions(localID) = globalID
  }


}

class Context(
    val parentContext:Option[Context],
    val varsToCreate :ListBuffer[Pair[String,VarRef]] = ListBuffer())
  extends AbstractContext {

  def addVarToCreate(tipe:String, globalName:VarRef) = varsToCreate += ((tipe, globalName))
}

