package planstack.anml.model

import planstack.anml.model.abs.Mod
import planstack.anml.{UnrecognizedExpression, VariableNotFound, ANMLException}
import planstack.anml.model.concrete.{Action => CAction}
import planstack.anml.model.concrete._
import planstack.anml.model.concrete.statements.Statement
import planstack.anml.parser._

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
abstract class AbstractContext(val pb:AnmlProblem) {

  def parentContext : Option[AbstractContext]
  val variables = mutable.Map[LVarRef, VarRef]()
  val nameToLocalVar = mutable.Map[String, LVarRef]()

  protected val actions = mutable.Map[LActRef, CAction]()
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

  def getNewUndefinedVar(typ: Type, refCounter: RefCounter) : LVarRef = {
    var i = 0
    while(nameToLocalVar.contains("locVar_"+i)) {
      i += 1
    }
    val v = new LVarRef("locVar_"+i, typ)
    addUndefinedVar(v, refCounter)
    v
  }

  def addUndefinedVar(name:LVarRef, refCounter: RefCounter)

  def bindVarToConstant(name:LVarRef, const:InstanceRef)

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

  def getType(localVarName: String) : Type = nameToLocalVar(localVarName).getType

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

  def hasLocalVar(name:String) : Boolean =
    nameToLocalVar.contains(name) || parentContext.exists(par => par.hasLocalVar(name))

  def getLocalVar(name:String) : LVarRef = {
    if(nameToLocalVar.contains(name))
      nameToLocalVar(name)
    else parentContext match {
      case Some(parent) => parent.getLocalVar(name)
      case None => throw new VariableNotFound(name)
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

  def getAction(localID:LActRef) : CAction = {
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
  def addAction(localID:LActRef, globalID:CAction) {
    assert(!actions.contains(localID) || actions(localID) == null)
    actions(localID) = globalID
  }

  def addActionCondition(localID:LActRef, globalDef:Task) {
    assert(!actions.contains(localID) || actions(localID) == null)
    assert(!tasks.contains(localID) || tasks(localID) == null)
    tasks(localID) = globalDef
  }

  val bindings : mutable.Map[EFunction,EVariable] = mutable.Map()
  private var nextBindingID = 0
  def bindingOf(f:EFunction, refCounter: RefCounter): EVariable = {
    assert(f.isConstant)
    assert(!f.func.valueType.isNumeric)
    if(!bindings.contains(f)) {
      bindings.put(f, EVariable("__binding_var__"+nextBindingID, f.func.valueType, Some(f)))
      addUndefinedVar(new LVarRef("__binding_var__"+nextBindingID, f.func.valueType), refCounter)
      nextBindingID += 1
    }
    bindings(f)
  }

  import planstack.anml.parser
  def simplify(e: parser.Expr, mod:Mod) : E = try {
    val simple = e match {
      case VarExpr(preModName) if hasLocalVar(mod.varNameMod(preModName)) =>
        val name = mod.varNameMod(preModName)
        EVariable(name, getLocalVar(name).getType, None)
      case VarExpr(name) if pb.functions.isDefined(name) =>
        EFunction(pb.functions.get(name), Nil)
      case FuncExpr(VarExpr(fName), args) if pb.functions.isDefined(fName) =>
        EFunction(pb.functions.get(fName), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
        case FuncExpr(VarExpr(tName), args) if pb.tasks.contains(tName) =>
        ETask(tName, args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
      case ChainedExpr(VarExpr(typ), second) if pb.instances.containsType(typ) =>
        second match {
          case VarExpr(sec) =>
            EFunction(pb.functions.get(s"$typ.$sec"), Nil)
          case FuncExpr(sec,args) =>
            EFunction(pb.functions.get(s"$typ.${sec.functionName}"), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case x =>
            sys.error("Second part of a chained expression should always be a func or a variable: "+x)
      }
      case ChainedExpr(left, right) =>
        val sleft = simplify(left,mod)
        (sleft, right) match {
          case (v@EVariable(_,typ, _), FuncExpr(fe, args)) =>
            val f = pb.functions.get(typ.getQualifiedFunction(fe.functionName))
            EFunction(f, v :: args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case (v@EVariable(_,typ, _), VarExpr(fname)) =>
            val f = pb.functions.get(typ.getQualifiedFunction(fname))
            EFunction(f, List(v))
          case x =>
            throw new ANMLException("Left part of chained expr was not reduced to variable: "+left)
        }
      case NumExpr(value) =>
        ENumber(value.toInt)
      case SetExpr(vals) =>
        ESet(vals.map(v => simplifyToVar(simplify(v,mod),mod)))
      case x => sys.error(s"Unrecognized expression: ${x.asANML}  --  $x")
    }
    simple match {
      case f:EFunction if f.isConstant => simplifyToVar(f,mod)
      case x => x
    }
  } catch {
    case exc:Throwable =>
      throw new UnrecognizedExpression(e, Some(exc))

  }

  private def simplifyToVar(e: E, mod: Mod) : EVariable = e match {
    case v:EVariable => v
    case f:EFunction if f.isConstant && !f.func.valueType.isNumeric =>
      bindingOf(f, pb.refCounter)
    case EFunction(f, args) if f.isConstant && f.valueType.isNumeric && !args.forall(a => a.expr.isEmpty) =>
      sys.error("Functions are not accepted as parameters of integer functions.")
    case ef@EFunction(f, args) if f.isConstant && f.valueType.isNumeric => // TODO this is a hack to make sure actions never end up with integer variables
      EVariable("xxxxxxxx"+{nextBindingID+=1;nextBindingID-1}, TInteger, Some(ef))
    case f:EFunction if !f.isConstant => throw new ANMLException("Trying to use "+f+" as a constant function.")
    case x => throw new ANMLException("Unrecognized expression: "+x)
  }

  def simplifyStatement(s: parser.Statement, mod: Mod) : EStatement = {
    def trans(id:String) = mod.idModifier(id)
    s match {
      case parser.SingleTermStatement(e, id) => simplify(e, mod) match {
        case f:EFunction =>
          assert(f.func.valueType.name == "boolean")
          EBiStatement(f, "==", EVariable("true", f.func.valueType, None), mod.idModifier(id))
        case v@EVariable(_,t:SimpleType,_) if t.name == "boolean" =>
          EBiStatement(v, "==", EVariable("true", t, None), mod.idModifier(id))
        case t:ETask =>
          EUnStatement(t, mod.idModifier(id))
        case x => sys.error("Problem: "+x)
      }
      case parser.TwoTermsStatement(e1, op, e2, id) =>
        EBiStatement(simplify(e1, mod), op.op, simplify(e2, mod), mod.idModifier(id))
      case parser.ThreeTermsStatement(e1,op1,e2,op2,e3,id) =>
        ETriStatement(simplify(e1, mod), op1.op, simplify(e2,mod), op2.op, simplify(e3,mod), mod.idModifier(id))
    }

  }
}

trait E
case class EVariable(name:String, typ:Type, expr:Option[EFunction]) extends E
case class EFunction(func:Function, args:List[EVariable]) extends E {
  def isConstant = func.isConstant
}
case class ENumber(n:Int) extends E
case class ETask(name:String, args:List[EVariable]) extends E
case class ESet(parts: Set[EVariable]) extends E

trait EStatement {
  require(id.nonEmpty, "Statement has an empty ID: "+this)
  def id : String
}
case class EUnStatement(e:E, id:String) extends EStatement
case class EBiStatement(e1:E, op:String, e2:E, id:String) extends EStatement
case class ETriStatement(e1:E, op:String, e2:E, op2:String, e3:E, id:String) extends EStatement

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
    pb:AnmlProblem,
    val parentContext:Option[Context],
    val varsToCreate :ListBuffer[VarRef] = ListBuffer())
  extends AbstractContext(pb) {

  var interval :TemporalInterval = null

  def setInterval(interval : TemporalInterval) { this.interval = interval}

  def addVarToCreate(globalVar:VarRef) = varsToCreate += globalVar

  override def addUndefinedVar(v: LVarRef, refCounter: RefCounter): Unit = {
    val globalVar = new VarRef(v.getType, refCounter)
    addVar(v, globalVar)
    addVarToCreate(globalVar)
  }

  def bindVarToConstant(name:LVarRef, const:InstanceRef): Unit = {
    assert(variables.contains(name))
    val previousGlobal = variables(name)
    varsToCreate -= previousGlobal
    variables.put(name, const)

  }
}

