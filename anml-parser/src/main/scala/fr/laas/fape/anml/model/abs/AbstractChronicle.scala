package fr.laas.fape.anml.model.abs

import java.util

import fr.laas.fape.anml.model._
import fr.laas.fape.anml.model.abs.statements._
import fr.laas.fape.anml.model.abs.time.{AbsTP, ContainerEnd, ContainerStart}
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.model.ir.{IRConstantExpression, IRSimpleVar}
import fr.laas.fape.anml.pending.IntExpression
import fr.laas.fape.anml.{ANMLException, FullSTN}
import planstack.structures.IList

import scala.collection.JavaConverters._

class AbstractChronicle(
                         private val statements: List[AbstractStatement],
                         private val constraints: List[AbstractConstraint],
                         private val variableDeclarations: List[IRSimpleVar],
                         val constantDeclarations: List[(IRSimpleVar,InstanceRef)],
                         val annotations: List[AbstractChronicleAnnotation],
                         val optSTNU: Option[AbstractSTNU]
                       )
{
  lazy val (implicitConstraints, implicitVariables) = {
    val funcVars = ((statements.flatMap(_.getAllVars) ++ constraints.flatMap(_.getAllVars)) collect {case x:IRConstantExpression => x}).toSet

    val additionalConstraints = funcVars.filterNot(v => v.getType.isNumeric)
      .map(v => {
        val sv = new AbstractParameterizedStateVariable(v.func, v.args)
        new AbstractEqualityConstraint(sv, v, LStatementRef(""))
      })
    (additionalConstraints, funcVars)
  }

  lazy val getStatements =  statements
  lazy val allConstraints = constraints ++ implicitConstraints

  def withStatements(otherStatements: AbstractStatement*) = withStatementsSeq(otherStatements.toList)
  def withStatementsSeq(otherStatements: Seq[AbstractStatement]) = {
    assert(optSTNU.isEmpty)
    new AbstractChronicle(statements ++ otherStatements, constraints, variableDeclarations, constantDeclarations, annotations, None)
  }

  def withConstraints(otherConstraints: AbstractConstraint*) = withConstraintsSeq(otherConstraints.toList)
  def withConstraintsSeq(otherConstraints: Seq[AbstractConstraint]) = {
    assert(optSTNU.isEmpty)
    new AbstractChronicle(statements, constraints ++ otherConstraints, variableDeclarations, constantDeclarations, annotations, None)
  }

  def withVariableDeclarations(vars: Seq[IRSimpleVar]) =
    new AbstractChronicle(statements, constraints, variableDeclarations ++ vars, constantDeclarations, annotations, None)

  def withConstantDeclarations(instances: Seq[(IRSimpleVar,InstanceRef)]) = {
    assert(optSTNU.isEmpty)
    new AbstractChronicle(statements, constraints, variableDeclarations, constantDeclarations ++ instances, annotations, None)
  }

  def +(annotation: AbstractChronicleAnnotation) =
    new AbstractChronicle(statements, constraints, variableDeclarations, constantDeclarations, annotation :: annotations, None)

  private def withSTNU(stnu: AbstractSTNU) =
    new AbstractChronicle(statements, constraints, variableDeclarations, constantDeclarations, annotations, Some(stnu))

  def +(absChron: AbstractChronicle) = union(absChron)

  def union(o: AbstractChronicle) : AbstractChronicle = {
    assert(optSTNU.isEmpty && o.optSTNU.isEmpty)
    new AbstractChronicle(
      statements ++ o.statements,
      constraints ++ o.constraints,
      variableDeclarations ++ o.variableDeclarations,
      constantDeclarations ++ o.constantDeclarations,
      annotations ++ o.annotations,
      None
    )
  }

  lazy val allVariablesToDeclare = (variableDeclarations ++ funcVars).toSet
  lazy val allVariables = (statements.flatMap(_.getAllVars) ++ constraints.flatMap(_.getAllVars)).distinct
  lazy val simpleVars = allVariables.collect({ case x:IRSimpleVar => x})
  lazy val funcVars = allVariables.collect({ case x:IRConstantExpression => x})
  lazy val check = {
    variableDeclarations.groupBy(v => v.name).values.collect { case g if g.size != 1 =>
      throw new ANMLException("This variable is declared more than once in the same scope: "+g.head.name) }

    true
  }

  /** Transform this chronicle into an equivalent one where temporal constraints are minimzed */
  def withMinialSTN(dispatchablePoints: List[AbsTP]) : AbstractChronicle = {
    if(optSTNU.nonEmpty)
      return this

    // minimize all temporal constraints and split timepoints between flexible and rigid (a rigid timepoint a a fixed delay wrt to a flexible)
    val simpleTempConst = constraints collect { case minDelay:AbstractMinDelay => minDelay }
    val otherConsts = constraints.filterNot(s => s.isInstanceOf[AbstractMinDelay])
    val timepoints = (statements.flatMap(s => List(s.start, s.end))
      ++ List(ContainerStart, ContainerEnd)
      ++ constraints.collect { case x: AbstractTemporalConstraint => x }.flatMap(c => c.timepoints)
      ).distinct

    // find all contingent timepoints
    val contingents = constraints.collect {
      case AbstractContingentConstraint(_, ctg, _, _) => ctg
    }
    val stn = new FullSTN(timepoints)
    for (AbstractMinDelay(from, to, minDelay) <- simpleTempConst)
      stn.addMinDelay(from, to, minDelay)
    logStatements.foreach {
      case t: AbstractTransition =>
        stn.addMinDelay(t.start, t.end, 1)
      case p: AbstractPersistence =>
        stn.addMinDelay(p.start, p.end, 0)
      case p: AbstractAssignment =>
        stn.addMinDelay(p.start, p.end, 1)
        stn.addMinDelay(p.end, p.start, -1)
    }

    val (flexs, newconstraints, anchored) = stn.minimalRepresentation(dispatchablePoints ++ (ContainerStart :: ContainerEnd :: contingents.toList))
    val stnu = new AbstractSTNU(
      dispatchablePoints.toSet,
      contingents.toSet,
      new IList(flexs),
      new IList(anchored.map(a => ActAnchoredTimepoint(a.timepoint, a.anchor, a.delay))),
      stn
    )

    new AbstractChronicle(statements, constraints, variableDeclarations, constantDeclarations, annotations, Some(stnu))
  }

  def getInstance(context: Context, temporalContext: TemporalInterval, pb: AnmlProblem, refCounter: RefCounter, optimizeTimepoints: Boolean = true) : Chronicle = {
    val chronicle = new Chronicle(temporalContext)

    for((local,global) <- constantDeclarations) {
      if(context.contains(local) && context.hasGlobalVar(local)) {
        assert(context.getGlobalVar(local) == global)
      } else {
        context.addVar(local, global)
      }
      chronicle.instances.add(global.instance)
    }

    for(v <- allVariablesToDeclare) {
      val newVar = new VarRef(v.getType, refCounter, Label(context.label, v.asANML))
      context.addVar(v, newVar)
      chronicle.vars.add(newVar)
    }

    for(v <- simpleVars)
      assert(context.hasGlobalVar(v), s"Variable $v is not defined in this scope")

    chronicle.addAllStatements(getStatements, context, pb, refCounter)
    chronicle.addAllConstraints(allConstraints, context, pb, refCounter)

    chronicle.annotations.addAll(annotations.map(_.getInstance(context, temporalContext, pb, refCounter)).asJava)
    chronicle
  }

  def subTasks = statements collect { case t:AbstractTask => t }
  def logStatements = statements collect { case s:AbstractLogStatement => s }

}

object EmptyAbstractChronicle extends AbstractChronicle(Nil, Nil, Nil, Nil, Nil, None)

class AbstractSTNU(val dispatchablePoints: Set[AbsTP],
                   val contingentPoints: Set[AbsTP],
                   val flexibleTimepoints: IList[AbsTP],
                   val anchoredTimepoints: IList[ActAnchoredTimepoint],
                   val stn: FullSTN[AbsTP]) {

}