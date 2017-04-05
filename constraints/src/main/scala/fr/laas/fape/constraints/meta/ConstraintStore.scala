package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.{Constraint, ReificationConstraint}
import fr.laas.fape.constraints.meta.events.{CSPEventHandler, Event, NewConstraintEvent, Satisfied}
import fr.laas.fape.constraints.meta.variables.IVar

import scala.collection.mutable

class ConstraintStore(_csp: CSP, toClone: Option[ConstraintStore]) extends CSPEventHandler {
  implicit val csp = _csp


  def this(_csp: CSP) = this(_csp, None)

  val active: mutable.Set[Constraint] = toClone match {
    case None => mutable.Set[Constraint]()
    case Some(base) => base.active.clone()
  }

  val satisfied: mutable.Set[Constraint] = toClone match {
    case None => mutable.Set[Constraint]()
    case Some(base) => base.satisfied.clone()
  }

  private val constraintsForVar: mutable.Map[IVar, mutable.Set[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.constraintsForVar.map(kv => (kv._1, kv._2.clone()))
  }

  /** Records a new active constraint and adds its variables to the index */
  private def record(constraint: Constraint) {
    active += constraint
    for(v <- constraint.variables) {
      constraintsForVar.getOrElseUpdate(v, mutable.Set()) += constraint
    }
  }

  /** Removes a constraint from the active list and removes it from the variable index */
  private def onSatisfaction(constraint: Constraint) {
    if(active.contains(constraint)) {
      assert(!satisfied.contains(constraint), s"Constraint $constraint already recorded as satisfied")
      active -= constraint
      satisfied += constraint
      for(v <- constraint.variables) {
        assert(constraintsForVar.contains(v) && constraintsForVar(v).contains(constraint))
        constraintsForVar(v) -= constraint
      }
    }
  }

  def watching(variable: IVar) : Iterable[Constraint] =
    constraintsForVar.getOrElse(variable, Nil)

  override def handleEvent(event: Event) {
    event match {
      case Satisfied(constraint) =>
        onSatisfaction(constraint)
      case NewConstraintEvent(constraint) =>
        record(constraint)
      case _ =>
    }
  }

  override def clone(newCSP: CSP): ConstraintStore = new ConstraintStore(newCSP, Some(this))
}
