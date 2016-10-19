package fr.laas.fape.anml.model

import fr.laas.fape.anml.model.abs.Mod
import fr.laas.fape.anml.model.concrete.statements.Statement
import fr.laas.fape.anml.model.concrete.{InstanceRef, RefCounter, _}
import fr.laas.fape.anml.model.ir._
import fr.laas.fape.anml.parser.{SetExpr, Action => _, _}
import fr.laas.fape.anml.{ANMLException, UnrecognizedExpression, VariableNotFound, parser}

import scala.collection.mutable

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

  def getNewUndefinedVar(typ: Type, refCounter: RefCounter) : IRSimpleVar = { //TODO: should be useless
    var i = 0
    while(nameToLocalVar.contains("locVar_"+i)) {
      i += 1
    }
    val v = IRSimpleVar("locVar_"+i, typ)
    addUndefinedVar(v)
    v
  }

  def addUndefinedVar(name: IRSimpleVar)

  def bindVarToConstant(name:LVarRef, const:InstanceRef)

  /**
   * @param localVar Name of the local variable to look up
   * @return a pair (type, globalName) of the local variable
   */
  protected def getDefinition(localVar:LVarRef) : VarRef = {
    assert(contains(localVar), s"Unknown local var $localVar")
    if(variables.contains(localVar)) {
      variables(localVar)
    } else {
      (parentContext,localVar) match {
        // Function vars are only checked in the local context, otherwise it could create problems
        // because of nested variables overloading each others
        case (Some(parent),_:IRSimpleVar) => parent.getDefinition(localVar)
        case _ => throw new ANMLException("Unable to find local var: "+localVar)
      }
    }
  }

  /** Checks if the local variable is defined in this context or its parent context. */
  def contains(localVar:LVarRef) : Boolean = {
    if(variables.contains(localVar)) {
      true
    } else {
      (parentContext,localVar) match {
        // Function vars are only checked in the local context, otherwise it could create problems
        // because of nested variables overloading each others
        case (Some(parent),_:IRSimpleVar) => parent.contains(localVar)
        case _ => false
      }
    }
  }

  def getType(localVarName: String) : Type = nameToLocalVar(localVarName).typ

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
    assert(!variables.contains(localName) || variables(localName).isEmpty, "Error: Context already contains local variable: "+localName)
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

  def simplify(e: Expr, mod:Mod) : IRExpression = try {
    val simple : IRExpression = e match {
      case VarExpr(name) if pb.functions.isDefined(name) && pb.functions.get(name).argTypes.isEmpty =>
        IRFunction.build(pb.functions.get(name), Nil)
      case VarExpr(preModName) =>
        val name = mod.varNameMod(preModName)
        getLocalVar(name).asInstanceOf[IRVar]
      case FuncExpr(VarExpr(fName), args) if pb.functions.isDefined(fName) =>
        IRFunction.build(pb.functions.get(fName), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
      case t@FuncExpr(VarExpr(tName), args) if pb.tasks.contains(tName) =>
        assert(args.size == pb.tasks(tName).size, s"Task `${t.asANML}` has the wrong number of arguments.")
        val simpleArgs = args.map(arg => simplifyToVar(simplify(arg,mod),mod))
        // check that the type of the variable is compatible with the expected type of the task argmuments
        for((t1, t2) <- simpleArgs.map(_.typ).zip(pb.tasks(tName).map(a => pb.instances.asType(a.tipe))))
          assert(t1.compatibleWith(t2), s"Type `$t1` and `$t2` are not compatible in the task: "+t)
        IRTask(tName, simpleArgs)
      case ChainedExpr(VarExpr(typ), second) if pb.instances.containsType(typ) =>
        second match {
          case VarExpr(sec) =>
            IRFunction.build(pb.functions.get(s"$typ.$sec"), Nil)
          case FuncExpr(sec,args) =>
            IRFunction.build(pb.functions.get(s"$typ.${sec.functionName}"), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case x =>
            sys.error("Second part of a chained expression should always be a func or a variable: "+x)
      }
      case ChainedExpr(left, right) =>
        val sleft = simplify(left,mod)
        (sleft, right) match {
          case (v:IRVar, FuncExpr(fe, args)) =>
            val f = pb.functions.get(v.typ.getQualifiedFunction(fe.functionName))
            IRFunction.build(f, v :: args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case (v:IRVar, VarExpr(fname)) =>
            val f = pb.functions.get(v.typ.getQualifiedFunction(fname))
            IRFunction.build(f, List(v))
          case x =>
            throw new ANMLException("Left part of chained expr was not reduced to variable: "+left)
        }
      case NumExpr(value) =>
        IRNumber(value.toInt)
      case SetExpr(vals) =>
        IRVarSet(vals.map(v => simplifyToVar(simplify(v,mod),mod)))
      case x => sys.error(s"Unrecognized expression: ${x.asANML}  --  $x")
    }
    simple match {
      case f:IRTimedFunction if f.isConstant => simplifyToVar(f,mod)
      case x => x
    }
  } catch {
    case exc:Throwable =>
      throw new UnrecognizedExpression(e, Some(exc))

  }

  private def simplifyToVar(e: IRExpression, mod: Mod) : IRVar = e match {
    case v:IRSimpleVar => v
    case f:IRConstantExpression if !f.func.valueType.isNumeric =>
      f
    case IRConstantExpression(f, args) if f.valueType.isNumeric && !args.forall(a => a.simpleVariable) =>
      sys.error("Functions are not accepted as parameters of integer functions.")
    case ef@IRConstantExpression(f, args) if f.valueType.isNumeric =>
      // TODO this is a hack to make sure actions never end up with integer variables
      //  EVariable("xxxxxxxx"+{nextBindingID+=1;nextBindingID-1}, TInteger, Some(ef))
      ef
    case f:IRTimedFunction if !f.isConstant => throw new ANMLException("Trying to use "+f+" as a constant function.")
    case x => throw new ANMLException("Unrecognized expression: "+x)
  }

  def simplifyStatement(s: parser.Statement, mod: Mod) : IRStatementGroup = {
    implicit def asSingletonGroup(eStatement: IRStatement) : IRStatementGroup = new IRSingletonGroup(eStatement)
    def trans(id:String) = mod.idModifier(id)
    s match {
      case parser.SingleTermStatement(e, id) => simplify(e, mod) match {
        case v:IRVar =>
          assert(v.getType.name == "boolean")
          IRBiStatement(v, "==", IRSimpleVar("true", v.getType), mod.idModifier(id))
        case t:IRTask =>
          IRUnStatement(t, mod.idModifier(id))
        case x => sys.error("Problem: "+x)
      }
      case parser.TwoTermsStatement(e1, op, e2, id) =>
        IRBiStatement(simplify(e1, mod), op.op, simplify(e2, mod), mod.idModifier(id))
      case parser.ThreeTermsStatement(e1,op1,e2,op2,e3,id) =>
        IRTriStatement(simplify(e1, mod), op1.op, simplify(e2,mod), op2.op, simplify(e3,mod), mod.idModifier(id))

      case parser.OrderedStatements(l, id) =>
        val simplified = l.map(s => simplifyStatement(s, mod))
        new IROrderedStatementGroup(simplified)
      case parser.UnorderedStatements(l, id) =>
        val simplified = l.map(s => simplifyStatement(s, mod))
        new IRUnorderedStatementGroup(simplified)
    }

  }
}
