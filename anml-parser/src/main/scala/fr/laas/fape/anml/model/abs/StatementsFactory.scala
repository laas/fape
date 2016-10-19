package fr.laas.fape.anml.model.abs

import fr.laas.fape.anml.model._
import fr.laas.fape.anml.model.abs.statements.{AbstractAssignment, AbstractLogStatement, AbstractPersistence, AbstractTransition}
import fr.laas.fape.anml.parser.{Expr, Statement, TemporalStatement}
import fr.laas.fape.anml.model._
import fr.laas.fape.anml.model.abs.statements._
import fr.laas.fape.anml.model.abs.time.AbstractTemporalAnnotation
import fr.laas.fape.anml.model.concrete.{InstanceRef, RefCounter}
import fr.laas.fape.anml.model.ir._
import fr.laas.fape.anml.parser.NumExpr
import fr.laas.fape.anml.pending.{IntExpression, IntLiteral}
import fr.laas.fape.anml.parser

trait Mod {
  def varNameMod(str: String) : String
  def idModifier(id: String) : String
}

object DefaultMod extends Mod {
  private var nextID = 0
  def varNameMod(name: String) = name
  def idModifier(id:String) =
    if(id.isEmpty) "__id__"+{nextID+=1; nextID-1}
    else id
}

object StatementsFactory {

  def asStateVariable(expr:Expr, context:AbstractContext, pb:AnmlProblem) = {
    AbstractParameterizedStateVariable(pb, context, expr)
  }

  /**
   * Transforms an annotated statement into its corresponding statement and the temporal constraints that applies
   * to its time-points (derived from the annotation).
   */
  def apply(annotatedStatement : TemporalStatement, context:AbstractContext, refCounter: RefCounter, mod: Mod = DefaultMod) : AbstractChronicle = {
    val  sg = StatementsFactory(annotatedStatement.statement, context, refCounter, mod) //TODO

    annotatedStatement.annotation match {
      case None =>
        sg.chronicle
      case Some(parsedAnnot) => {
        assert(sg.statements.nonEmpty, "Temporal annotation on something that is not a statement nor a task: "+sg)
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        new AnnotatedStatementGroup(annot, sg).chronicle
      }
    }
  }

  def apply(statement : Statement, context:AbstractContext, refCounter: RefCounter, mod: Mod) : AbstractChronicleGroup = {
    def asSv(f:IRFunction) = new AbstractParameterizedStateVariable(f.func, f.args)
    def asRef(id:String) = LStatementRef(id)
    val eGroup = context.simplifyStatement(statement, mod)
    val EMPTY = EmptyAbstractChronicle
    def trans(e:IRStatement) : AbstractChronicle = e match {
      case IRTriStatement(f:IRTimedFunction, "==", v1:IRVar, ":->", v2:IRVar, id) =>
        EMPTY.withStatements(new AbstractTransition(asSv(f), v1, v2, asRef(id)))

      case IRBiStatement(vleft: IRConstantExpression, ":=", value:IRSimpleVar, id) =>
        assert(context.hasGlobalVar(value), s"$value is not defined yet when assigned to $vleft")
        assert(context.getGlobalVar(value).isInstanceOf[InstanceRef], s"$value is not recognied as a constant symbol when assigned to $vleft")
        EMPTY.withConstraints(new AbstractAssignmentConstraint(asSv(vleft), value, asRef(id)))
          .withConstraints(new AbstractVarEqualityConstraint(vleft, value, asRef(id)))

      case IRBiStatement(vleft :IRConstantExpression, ":=", IRNumber(value), id) =>
        EMPTY.withConstraints(new AbstractIntAssignmentConstraint(asSv(vleft), value, asRef(id)))

      case IRBiStatement(vleft:IRVar, "==", vright:IRVar, id) =>
        EMPTY.withConstraints(new AbstractVarEqualityConstraint(vleft, vright, asRef(id)))

      case IRBiStatement(vleft:IRVar, "!=", vright:IRVar, id) =>
        EMPTY.withConstraints(new AbstractVarInequalityConstraint(vleft, vright, asRef(id)))

      case IRBiStatement(f:IRTimedFunction, ":=", value:IRVar, id) =>
        assert(!f.func.isConstant)
        EMPTY.withStatements(new AbstractAssignment(asSv(f), value, asRef(id)))

      case IRBiStatement(f:IRTimedFunction, "==", value:IRVar, id) =>
        EMPTY.withStatements(new AbstractPersistence(asSv(f), value, asRef(id)))

      case IRBiStatement(f:IRTimedFunction, "!=", value:IRVar, id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        EMPTY.withVariableDeclarations(intermediateVar :: Nil)
          .withStatements(new AbstractPersistence(asSv(f), intermediateVar, asRef(id)))
          .withConstraints(new AbstractVarInequalityConstraint(intermediateVar, value, LStatementRef("")))

      case IRUnStatement(IRTask(t, args), id) =>
        EMPTY.withStatements(new AbstractTask("t-"+t, args, LActRef(id)))

      case IRBiStatement(v1:IRVar, "in", right@IRVarSet(vars), id) =>
        EMPTY.withConstraints(new AbstractInConstraint(v1, vars.map(x => x), asRef(id)))

      case IRBiStatement(f:IRTimedFunction, "in", right@IRVarSet(vars), id) =>
        val intermediateVar = context.getNewUndefinedVar(f.func.valueType, refCounter)
        EMPTY.withVariableDeclarations(intermediateVar :: Nil)
          .withStatements(new AbstractPersistence(asSv(f), intermediateVar, asRef(id)))
          .withConstraints(new AbstractInConstraint(intermediateVar, vars.map(x => x), asRef(id)))

      case x => sys.error("Unmatched: "+x)
    }
    eGroup.process(trans)
  }

}
