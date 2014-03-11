package planstack.anml.model

import planstack.anml.model.{AbstractContext, VariableFactory}

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

  /**
   * Builds a new concrete context (i.e. all local vars map to a global var) by
   *  - adding all (local, global) variable pairs to the new context.
   *  - creating the missing global variables using `factory`
   * @param parent Concrete context to be added as the parent of the built context
   * @param factory VariableFactory for creating new vars.
   * @param newVars map of (localVar -> globalVar) to be added to the context)
   * @return
   */
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
