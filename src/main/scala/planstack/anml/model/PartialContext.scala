package planstack.anml.model


class PartialContext(val parentContext:Option[AbstractContext]) extends AbstractContext {

  def addUndefinedVar(name:LVarRef, typeName:String) {
    assert(!variables.contains(name), "Local variable already defined: "+name)
    variables.put(name, (typeName, new VarRef("")))
  }

  def addUndefinedAction(localID:LActRef) {
    assert(!actions.contains(localID))
    actions.put(localID, new ActRef(""))
  }

  /**
   * Creates a new local var with type tipe. Returns the name of the created variable.
   * @param tipe Type of the variable to create
   * @return Name of the new local variable
   */
  def getNewLocalVar(tipe:String) : LVarRef = {
    var i = 0
    var lVarRef = new LVarRef("locVar_"+i)
    while(contains(lVarRef)) {
      i += 1
      lVarRef = new LVarRef("locVar_"+i)
    }
    addUndefinedVar(lVarRef, tipe)
    lVarRef
  }

  /**
   * Builds a new concrete context (i.e. all local vars map to a global var) by
   *  - adding all (local, global) variable pairs to the new context.
   *  - creating the missing global variables using `factory`
   * @param parent Concrete context to be added as the parent of the built context
   * @param newVars map of (localVar -> globalVar) to be added to the context)
   * @return
   */
  def buildContext(pb:AnmlProblem, parent:Option[Context], newVars:Map[LVarRef, VarRef] = Map()) = {
    val context = new Context(parent)

    for((local, (tipe, global)) <- variables) {
      if(global.isEmpty && newVars.contains(local)) {
        context.addVar(local, tipe, newVars(local))
      } else if(global.isEmpty) {
        val globalName = pb.newGlobalVar
        context.addVar(local, tipe, globalName)
        context.addVarToCreate(tipe, globalName)
      } else {
        context.addVar(local, tipe, global)
      }
    }

    for((localActionID, globalActionID) <- actions) {
      if(globalActionID.isEmpty)
        context.addActionID(localActionID, new ActRef(pb.newActionID))
      else
        context.addActionID(localActionID, globalActionID)
    }
    context
  }

}
