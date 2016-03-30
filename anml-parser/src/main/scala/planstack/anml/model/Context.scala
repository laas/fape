package planstack.anml.model

import planstack.anml.ANMLException
import planstack.anml.model.concrete._
import planstack.anml.model.concrete.statements.Statement

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * A context defines mapping between local references appearing in abstract objects and
 * global references appearing in concrete objects. Those context are defined for the problem,
 * actions and decompositions.
 *
 * It optionally refers to a parent context where more mappings might be defined.
 *
 *
 */
abstract class AbstractContext {

  def parentContext : Option[AbstractContext]
  val variables = mutable.Map[LVarRef, VarRef]()
  val nameToLocalVar = mutable.Map[String, LVarRef]()

  protected val actions = mutable.Map[LActRef, Action]()
  protected val tasks = mutable.Map[LActRef, Task]()

  protected val statements = mutable.Map[LStatementRef, Statement]()

  def getIntervalWithID(ref:LocalRef) : TemporalInterval = {
    if(actions.contains(new LActRef(ref.id)) && !tasks.contains(new LActRef(ref.id))) {
      //TODO above line is a ugly hack
      actions(new LActRef(ref.id))
    } else if(statements.contains(new LStatementRef(ref.id))) {
      statements(new LStatementRef(ref.id))
    } else if(tasks.contains(new LActRef(ref.id))) {
      tasks(new LActRef(ref.id))
    } else {
      parentContext match {
        case Some(context) => context.getIntervalWithID(ref)
        case None => throw new ANMLException("Unable to find an interval with ID: "+ref)
      }
    }
  }
  protected val standaloneTimepoints = mutable.Map[String, TPRef]()

  def getTimepoint(id: String, refCounter: RefCounter) = {
    assert(id != "start" && id != "end")
    standaloneTimepoints.getOrElseUpdate(id, { new TPRef(refCounter) })
  }

  def getNewUndefinedVar(typ: String, refCounter: RefCounter) : LVarRef = {
    var i = 0
    while(nameToLocalVar.contains("locVar_"+i)) {
      i += 1
    }
    val v = new LVarRef("locVar_"+i, typ)
    addUndefinedVar(v, typ, refCounter)
    v
  }

  def addUndefinedVar(name:LVarRef, typeName:String, refCounter: RefCounter)

  /**
   * @param localName Name of the local variable to look up
   * @return a pair (type, globalName) of the local variable
   */
  protected def getDefinition(localName:LVarRef) : VarRef = {
    if(variables.contains(localName)) {
      variables(localName)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unable to find local var: "+localName)
        case Some(parent) => parent.getDefinition(localName)
      }
    }
  }

  /** Checks if the local variable is defined in this context or its parent context. */
  def contains(localName:LVarRef) : Boolean = {
    if(variables.contains(localName)) {
      true
    } else {
      parentContext match {
        case None => false
        case Some(parent) => parent.contains(localName)
      }
    }
  }

  /** Looks up for the type of the local variable.
    * 
    * @param localRef Reference to the local variable to look up.
    * @return The type of this local variable. Trows an ANMLException if this variable is
    *         not defined.
    */
  def getType(localRef:LVarRef) : String = {
    val t = getDefinition(localRef).typ
    assert(t == localRef.typ)
    t
  }

  def getType(localVarName: String) : String = nameToLocalVar(localVarName).typ

  /** Looks up the global reference associated to this local variable.
    * 
    * @param localRef Reference to the local variable to look up.
    * @return The global variable reference associated with this local variable. Throws an ANMLException if this
    *         local variable is not defined.
    */
  def getGlobalVar(localRef:LVarRef) : VarRef = {
    val (globalVar) = getDefinition(localRef)
    if(globalVar.isEmpty)
      throw new ANMLException("Variable %s has no global definition".format(localRef))
    else
      globalVar
  }

  def hasGlobalVar(localRef: LVarRef) : Boolean =
    getDefinition(localRef).nonEmpty

  def getLocalVar(name:String) : LVarRef = {
    if(nameToLocalVar.contains(name))
      nameToLocalVar(name)
    else parentContext match {
      case Some(parent) => parent.getLocalVar(name)
      case None => throw new ANMLException("Unable to find variable: "+name)
    }
  }

  def getLocalVar(globalRef: VarRef) : LVarRef = {
    for((lv, v) <- variables ; if v == globalRef)
      return lv

    parentContext match {
      case Some(parent) => parent.getLocalVar(globalRef)
      case None => null
    }
  }

  def addVar(localName:LVarRef, globalName:VarRef) {
    assert(!variables.contains(localName), "Error: Context already contains local variable: "+localName)
    nameToLocalVar.put(localName.id, localName)
    variables.put(localName, globalName)
  }

  def getAction(localID:LActRef) : Action = {
    if(actions.contains(localID)) {
      actions(localID)
    } else {
      parentContext match {
        case None => throw new ANMLException("Unknown action local ID: "+localID)
        case Some(parent) => parent.getAction(localID)
      }
    }
  }

  def addStatement(localRef:LStatementRef, statement:Statement) {
    assert(!statements.contains(localRef) || statements(localRef) == null)
    statements.put(localRef, statement)
  }

  def getStatement(localRef:LStatementRef) : Statement = {
    assert(statements.contains(localRef) && statements(localRef) != null)
    statements(localRef)
  }
  def getRefOfStatement(statement: Statement) : LStatementRef =
    statements.find(_._2 == statement) match {
      case Some((ref, statement2)) => ref
      case None => throw new ANMLException("Unable to find reference of statement "+statement)
  }

  def contains(statement: Statement) : Boolean =
    statements.find(_._2 == statement) match {
      case Some(_) => true
      case None => false
    }

  /** Adds both the local and global reference to an AbstractAction/Action
    *
    * @param localID Local reference of the AbstractAction
    * @param globalID Global reference of the Action
    */
  def addAction(localID:LActRef, globalID:Action) {
    assert(!actions.contains(localID) || actions(localID) == null)
    actions(localID) = globalID
  }

  def addActionCondition(localID:LActRef, globalDef:Task) {
    assert(!actions.contains(localID) || actions(localID) == null)
    assert(!tasks.contains(localID) || tasks(localID) == null)
    tasks(localID) = globalDef
  }


}

/** A context where all references are fully defined (i.e. every local reference has a corresponding global reference).
  *
  * {{{
  *   // Definition of the action
  *   action Move(Location a, Location b) {
  *     ...
  *   };
  *
  *   // Reference of the action, where LA is an instance of type Location
  *   Move(LA, any_)
  * }}}
  *
  * The previous example would give an [[planstack.anml.model.concrete.Action]] with the following Context:
  *
  *  - parentContext: `Some(anmlProblem)`
  *
  *  - variables : `{ a -> (Location, LA, b -> (Location, any_ }`
  *
  *  - actions: {}
  *
  *  - varsToCreate: `{(Location, any_)}`
  *
  * @param parentContext An optional parent context. If given it has to be a [[planstack.anml.model.Context]] (ie fully defined).
  * @param varsToCreate All (Type, VarRef) pair that need to be created such that every global variable mentionned in this context
  *                     exists in the state.
  */
class Context(
    val parentContext:Option[Context],
    val varsToCreate :ListBuffer[VarRef] = ListBuffer())
  extends AbstractContext {

  var interval :TemporalInterval = null

  def setInterval(interval : TemporalInterval) { this.interval = interval}

  def addVarToCreate(globalVar:VarRef) = varsToCreate += globalVar

  override def addUndefinedVar(name: LVarRef, typeName: String, refCounter: RefCounter): Unit = {
    val globalVar = new VarRef(typeName, refCounter)
    assert(name.typ == typeName)
    addVar(name, globalVar)
    addVarToCreate(globalVar)
  }
}

