package planstack.anml.model

import planstack.anml.model.abs.time.{AbstractTemporalAnnotation, IntervalEnd, IntervalStart}
import planstack.anml.model.abs._
import planstack.anml.model.abs.statements.{AbstractAssignment, AbstractStatement, AbstractTransition}
import planstack.anml.pending.IntExpression
import planstack.anml.{ANMLException, UnrecognizedExpression, VariableNotFound}
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

  def getNewUndefinedVar(typ: Type, refCounter: RefCounter) : LVarRef = { //TODO: should be useless
    var i = 0
    while(nameToLocalVar.contains("locVar_"+i)) {
      i += 1
    }
    val v = EVariable("locVar_"+i, typ)
    addUndefinedVar(v)
    v
  }

  def addUndefinedVar(name: EVariable)

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
        case (Some(parent),_:EVariable) => parent.getDefinition(localVar)
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
        case (Some(parent),_:EVariable) => parent.contains(localVar)
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

//  val bindings : mutable.Map[EFunction,EVariable] = mutable.Map()
//  private var nextBindingID = 0
//  def bindingOf(f:EFunction, refCounter: RefCounter): EVariable = {
//    assert(f.isConstant)
//    assert(!f.func.valueType.isNumeric)
//    if(!bindings.contains(f)) {
//      bindings.put(f, EVariable("__binding_var__"+nextBindingID, f.func.valueType, Some(f)))
//      addUndefinedVar(new LVarRef("__binding_var__"+nextBindingID, f.func.valueType), refCounter)
//      nextBindingID += 1
//    }
//    bindings(f)
//  }

  import planstack.anml.parser
  def simplify(e: parser.Expr, mod:Mod) : E = try {
    val simple : E = e match {
      case VarExpr(name) if pb.functions.isDefined(name) =>
        EFunction.build(pb.functions.get(name), Nil)
      case VarExpr(preModName) =>
        val name = mod.varNameMod(preModName)
        getLocalVar(name).asInstanceOf[EVar]
      case FuncExpr(VarExpr(fName), args) if pb.functions.isDefined(fName) =>
        EFunction.build(pb.functions.get(fName), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
      case t@FuncExpr(VarExpr(tName), args) if pb.tasks.contains(tName) =>
        assert(args.size == pb.tasks(tName).size, s"Task `${t.asANML}` has the wrong number of arguments.")
        val simpleArgs = args.map(arg => simplifyToVar(simplify(arg,mod),mod))
        // check that the type of the variable is compatible with the expected type of the task argmuments
        for((t1, t2) <- simpleArgs.map(_.typ).zip(pb.tasks(tName).map(a => pb.instances.asType(a.tipe))))
          assert(t1.compatibleWith(t2), s"Type `$t1` and `$t2` are not compatible in the task: "+t)
        ETask(tName, simpleArgs)
      case ChainedExpr(VarExpr(typ), second) if pb.instances.containsType(typ) =>
        second match {
          case VarExpr(sec) =>
            EFunction.build(pb.functions.get(s"$typ.$sec"), Nil)
          case FuncExpr(sec,args) =>
            EFunction.build(pb.functions.get(s"$typ.${sec.functionName}"), args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case x =>
            sys.error("Second part of a chained expression should always be a func or a variable: "+x)
      }
      case ChainedExpr(left, right) =>
        val sleft = simplify(left,mod)
        (sleft, right) match {
          case (v:EVar, FuncExpr(fe, args)) =>
            val f = pb.functions.get(v.typ.getQualifiedFunction(fe.functionName))
            EFunction.build(f, v :: args.map(arg => simplifyToVar(simplify(arg,mod),mod)))
          case (v:EVar, VarExpr(fname)) =>
            val f = pb.functions.get(v.typ.getQualifiedFunction(fname))
            EFunction.build(f, List(v))
          case x =>
            throw new ANMLException("Left part of chained expr was not reduced to variable: "+left)
        }
      case NumExpr(value) =>
        ENumber(value.toInt)
      case SetExpr(vals) =>
        EVarSet(vals.map(v => simplifyToVar(simplify(v,mod),mod)))
      case x => sys.error(s"Unrecognized expression: ${x.asANML}  --  $x")
    }
    simple match {
      case f:ETimedFunction if f.isConstant => simplifyToVar(f,mod)
      case x => x
    }
  } catch {
    case exc:Throwable =>
      throw new UnrecognizedExpression(e, Some(exc))

  }

  private def simplifyToVar(e: E, mod: Mod) : EVar = e match {
    case v:EVariable => v
    case f:EConstantFunction if !f.func.valueType.isNumeric =>
      f
    case EConstantFunction(f, args) if f.valueType.isNumeric && !args.forall(a => a.simpleVariable) =>
      sys.error("Functions are not accepted as parameters of integer functions.")
    case ef@EConstantFunction(f, args) if f.valueType.isNumeric =>
      // TODO this is a hack to make sure actions never end up with integer variables
      //  EVariable("xxxxxxxx"+{nextBindingID+=1;nextBindingID-1}, TInteger, Some(ef))
      ef
    case f:ETimedFunction if !f.isConstant => throw new ANMLException("Trying to use "+f+" as a constant function.")
    case x => throw new ANMLException("Unrecognized expression: "+x)
  }

  def simplifyStatement(s: parser.Statement, mod: Mod) : EStatementGroup = {
    implicit def asSingletonGroup(eStatement: EStatement) : EStatementGroup = new ESingletonGroup(eStatement)
    def trans(id:String) = mod.idModifier(id)
    s match {
      case parser.SingleTermStatement(e, id) => simplify(e, mod) match {
        case v:EVar =>
          assert(v.getType.name == "boolean")
          EBiStatement(v, "==", EVariable("true", v.getType), mod.idModifier(id))
        case t:ETask =>
          EUnStatement(t, mod.idModifier(id))
        case x => sys.error("Problem: "+x)
      }
      case parser.TwoTermsStatement(e1, op, e2, id) =>
        EBiStatement(simplify(e1, mod), op.op, simplify(e2, mod), mod.idModifier(id))
      case parser.ThreeTermsStatement(e1,op1,e2,op2,e3,id) =>
        ETriStatement(simplify(e1, mod), op1.op, simplify(e2,mod), op2.op, simplify(e3,mod), mod.idModifier(id))

      case parser.OrderedStatements(l, id) =>
        val simplified = l.map(s => simplifyStatement(s, mod))
        new EOrderedStatementGroup(simplified)
      case parser.UnorderedStatements(l, id) =>
        val simplified = l.map(s => simplifyStatement(s, mod))
        new EUnorderedStatementGroup(simplified)
    }

  }
}

trait E
trait EVar extends E with LVarRef with VarContainer {
  def typ: Type
  def simpleVariable : Boolean
}
case class EVariable(name:String, typ:Type) extends EVar {
  def simpleVariable = true
  override def id: String = name
  override def getAllVars: Set[LVarRef] = Set(this)
  override def asANML: String = name
  override def toString = asANML
}

trait EFunction extends E {
  def func:Function
  def args:List[EVar]
}
object EFunction {
  def build(func:Function, args:List[EVar]) = func.isConstant match {
    case true => EConstantFunction(func, args)
    case false => ETimedFunction(func, args)
  }
}

case class EConstantFunction(func:Function, args:List[EVar]) extends EVar with EFunction {
  require(isConstant)
  def isConstant = func.isConstant
  def simpleVariable = false
  def id = asANML
  override def typ: Type = func.valueType
  override def getAllVars: Set[LVarRef] = args.flatMap(a => a.getAllVars).toSet + this
  override def asANML: String = s"${func.name}(${args.map(_.asANML).mkString(",")})"
  override def toString = asANML
}

case class ETimedFunction(func:Function, args:List[EVar]) extends EFunction{
  require(!isConstant)
  def typ = func.valueType
  def isConstant = func.isConstant
}
case class ENumber(n:Int) extends E
case class ETask(name:String, args:List[EVar]) extends E
case class EVarSet(parts: Set[EVar]) extends E
case class Timepoint()

trait EStatement {
  require(id.nonEmpty, "Statement has an empty ID: "+this)
  def id : String
}
case class EUnStatement(e:E, id:String) extends EStatement
case class EBiStatement(e1:E, op:String, e2:E, id:String) extends EStatement
case class ETriStatement(e1:E, op:String, e2:E, op2:String, e3:E, id:String) extends EStatement

abstract class EStatementGroup {
  def firsts : List[EStatement]
  def lasts : List[EStatement]
  def statements : List[EStatement]
  def process(f : (EStatement => AbstractChronicle)) : AbsStatementGroup
}
class ESingletonGroup(val statement: EStatement) extends EStatementGroup {
  override def firsts: List[EStatement] = List(statement)
  override def lasts: List[EStatement] = List(statement)
  override def statements: List[EStatement] = List(statement)
  override def process(f: (EStatement) => AbstractChronicle): AbsStatementGroup = {
    val ac = f(statement)
    new LeafGroup(ac.getStatements,ac.allConstraints)
  }
}
class EOrderedStatementGroup(parts: List[EStatementGroup]) extends EStatementGroup {
  override def firsts: List[EStatement] = parts.head.firsts
  override def lasts: List[EStatement] = parts.last.lasts
  override def statements: List[EStatement] = parts.flatMap(_.statements)
  override def process(f: (EStatement) => AbstractChronicle): AbsStatementGroup =
    new OrderedGroup(parts.map(_.process(f)))
}
class EUnorderedStatementGroup(parts: List[EStatementGroup]) extends EStatementGroup {
  override def firsts: List[EStatement] = parts.flatMap(_.firsts)
  override def lasts: List[EStatement] = parts.flatMap(_.lasts)
  override def statements: List[EStatement] = parts.flatMap(_.statements)
  override def process(f: (EStatement) => AbstractChronicle): AbsStatementGroup =
  new UnorderedGroup(parts.map(_.process(f)))
}

abstract class AbsStatementGroup {
  def firsts : List[AbstractStatement]
  def lasts : List[AbstractStatement]
  def statements : List[AbstractStatement]
  def constraints = getStructuralTemporalConstraints ::: baseConstraints
  def baseConstraints : List[AbstractConstraint]

  def getStructuralTemporalConstraints : List[AbstractMinDelay]

  /** Produces the temporal constraints by applying the temporal annotation to this statement. */
  def getTemporalConstraints(annot : AbstractTemporalAnnotation) : List[AbstractMinDelay] = {
    annot match {
      case AbstractTemporalAnnotation(s, e, "is") =>
        assert(firsts.size == 1, s"Cannot apply the temporal annotation $annot on unordered statemers $firsts. " +
          s"Maybe a 'contains' is missing.")
        (firsts flatMap {
          case ass:AbstractAssignment => // assignment is a special case: any annotation is always applied to end timepoint
            assert(s == e, "Non instantaneous assignment.")
            AbstractExactDelay(s.timepoint, ass.end, IntExpression.lit(s.delta)) ++
              AbstractExactDelay(ass.start, ass.end, IntExpression.lit(1))
          case x =>
            AbstractExactDelay(s.timepoint, x.start, IntExpression.lit(s.delta))
        }) ++
          (lasts flatMap { x =>
              AbstractExactDelay(e.timepoint, x.end, IntExpression.lit(e.delta))
          })
      case AbstractTemporalAnnotation(s, e, "contains") =>
        (firsts map {
          case ass:AbstractAssignment =>
            throw new ANMLException("The 'contains' keyword is not allowed on assignments becouse it would introduce disjunctive effects: "+ass)
          case tr:AbstractTransition =>
            throw new ANMLException("The 'contains' keyword is not allowed on transitions becouse it would introduce disjunctive effects: "+tr)
          case x =>
            AbstractMinDelay(s.timepoint, x.start, IntExpression.lit(s.delta)) // start(id) >= start+delta
        }) ++
          (lasts map { x =>
            AbstractMinDelay(x.end, e.timepoint, IntExpression.lit(-e.delta)) // end(id) <= end+delta
          })
    }
  }
}
class LeafGroup(val statements:List[AbstractStatement], val baseConstraints: List[AbstractConstraint]) extends AbsStatementGroup {
  override def firsts: List[AbstractStatement] = statements
  override def lasts: List[AbstractStatement] = statements
  override def getStructuralTemporalConstraints: List[AbstractMinDelay] = Nil
}
class OrderedGroup(val parts: List[AbsStatementGroup]) extends AbsStatementGroup {
  override def firsts: List[AbstractStatement] = parts.head.firsts
  override def lasts: List[AbstractStatement] = parts.last.lasts
  override def statements: List[AbstractStatement] = parts.flatMap(_.statements)
  override def baseConstraints: List[AbstractConstraint] = parts.flatMap(_.baseConstraints)
  override def getStructuralTemporalConstraints: List[AbstractMinDelay] = {
    def constraintsBetween(p1:AbsStatementGroup, p2:AbsStatementGroup) =
      for(e <- p1.lasts ; s <- p2.firsts)
        yield AbstractMinDelay(e.end, s.start, IntExpression.lit(0))
    val inter = parts.tail.foldLeft[(AbsStatementGroup, List[AbstractMinDelay])]((parts.head, Nil))(
      (acc, cur) => (cur, acc._2 ::: constraintsBetween(acc._1,cur)))._2
    val intra = parts.flatMap(_.getStructuralTemporalConstraints)
    inter ++ intra
  }
}
class UnorderedGroup(val parts: List[AbsStatementGroup]) extends AbsStatementGroup {
  override def firsts: List[AbstractStatement] = parts.flatMap(_.firsts)
  override def lasts: List[AbstractStatement] = parts.flatMap(_.lasts)
  override def statements: List[AbstractStatement] = parts.flatMap(_.statements)
  override def baseConstraints: List[AbstractConstraint] = parts.flatMap(_.baseConstraints)
  override def getStructuralTemporalConstraints: List[AbstractMinDelay] = parts.flatMap(_.getStructuralTemporalConstraints)
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
  */
class Context(
    pb:AnmlProblem,
    val label: String,
    val parentContext:Option[Context],
    val interval: TemporalInterval)
  extends AbstractContext(pb) {

  override def addUndefinedVar(v: EVariable): Unit = {
    assert(!variables.contains(v), "Local variable already defined: "+v)
    assert(!nameToLocalVar.contains(v.name), "Local variable already recorded: "+v)
    nameToLocalVar. += ((v.name, v))
  }

  def bindVarToConstant(name:LVarRef, const:InstanceRef): Unit = {
    assert(variables.contains(name))
    val previousGlobal = variables(name)
    variables.put(name, const)
  }
}

