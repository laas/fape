package planstack.anml.model

import scala.collection.mutable
import planstack.anml.ANMLException


abstract class AbstractContext {

  def parentContext : Option[AbstractContext]
  val variables = mutable.Map[String, Pair[String, String]]()


  def getGlobalVar(localName:String) : String = {
    if(variables.contains(localName)) {
      val (tipe, globalName) = variables(localName)
      if(globalName.isEmpty)
        throw new ANMLException("Global name for %s does not exists.".format(localName))
      else
        globalName
    } else {
      parentContext match {
        case None => throw new ANMLException("Unable to find local var: "+localName)
        case Some(parent) => parent.getGlobalVar(localName)
      }
    }
  }

  def addVar(localName:String, typeName:String, globalName:String) {
    assert(!variables.contains(localName))
    variables.put(localName, (typeName, globalName))
  }
}

class Context(val parentContext:Option[Context]) extends AbstractContext {


}

class PartialContext(val parentContext:Option[AbstractContext]) extends AbstractContext {


  def addUndefinedVar(name:String, typeName:String) {
    assert(!variables.contains(name))
    variables.put(name, (typeName, ""))
  }


}