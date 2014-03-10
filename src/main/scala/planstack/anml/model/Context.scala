package planstack.anml.model

import scala.collection.mutable
import planstack.anml.ANMLException


abstract class AbstractContext {

  def parentContext : Option[AbstractContext]
  protected val variables = mutable.Map[String, Pair[String, String]]()

  protected val actions = mutable.Map[String, String]()

  /**
   * @param localName Name of the local variable to look up
   * @return a pair (type, globalName) of the local variable
   */
  protected def getDefinition(localName:String) : Pair[String, String] = {
    if(variables.contains(localName)) {
      variables(localName)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unable to find local var: "+localName)
        case Some(parent) => parent.getDefinition(localName)
      }
    }
  }

  def contains(localName:String) = {
    try {
      getDefinition(localName)
      true
    } catch {
      case _:Exception => false
    }
  }

  def getType(localName:String) : String = getDefinition(localName)._1

  def getGlobalVar(localName:String) : String = {
    val (tipe, globalName) = getDefinition(localName)
    if(globalName.isEmpty)
      throw new ANMLException("Variable %s has no global definition".format(localName))
    else
      globalName
  }

  def addVar(localName:String, typeName:String, globalName:String) {
    assert(!variables.contains(localName))
    variables.put(localName, (typeName, globalName))
  }

  def getActionID(localID:String) : String = {
    if(actions.contains(localID)) {
      actions(localID)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unknown action local ID: "+localID)
        case Some(parent) => parent.getActionID(localID)
      }
    }
  }

  def addActionID(localID:String, globalID:String) {
    assert(!actions.contains(localID) || actions(localID).isEmpty)
    actions(localID) = globalID
  }


}

class Context(val parentContext:Option[Context]) extends AbstractContext {


}

class PartialContext(val parentContext:Option[AbstractContext]) extends AbstractContext {

  def addUndefinedVar(name:String, typeName:String) {
    assert(!variables.contains(name))
    variables.put(name, (typeName, ""))
  }

  def addUndefinedAction(localID:String) {
    assert(!actions.contains(localID))
    actions.put(localID, "")
  }

  /**
   * Creates a new local var with type tipe. Returns the name of the created variable.
   * @param tipe Type of the variable to create
   * @return Name of the new local variable
   */
  def getNewLocalVar(tipe:String) : String = {
    var i = 0
    while(contains("locVar"+i+"__"))
      i += 1
    addUndefinedVar("locVar"+i+"__", tipe)
    "locVar"+i+"__"
  }

  def buildContext(parent:Option[Context], factory:VariableFactory, newVars:Map[String, String] = Map()) = {
    val context = new Context(parent)

    for((local, (tipe, global)) <- variables) {
      if(global.isEmpty && newVars.contains(local))
        context.addVar(local, tipe, newVars(local))
      else if(global.isEmpty)
        context.addVar(local, tipe, factory.createVar(tipe))
      else
        context.addVar(local, tipe, global)
    }
    context
  }

}