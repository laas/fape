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

