package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.{Constraint, ReificationConstraint}
import fr.laas.fape.constraints.meta.events._
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

  private val activeConstraintsForVar: mutable.Map[IVar, mutable.Set[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.activeConstraintsForVar.map(kv => (kv._1, kv._2.clone()))
  }

  private val watchedConstraintsForVar: mutable.Map[IVar, mutable.Set[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watchedConstraintsForVar.map(kv => (kv._1, kv._2.clone()))
  }

  private val watchers: mutable.Map[Constraint, mutable.Set[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watchers.map(kv => (kv._1, kv._2.clone()))
  }

  private val watches: mutable.Map[Constraint, mutable.Set[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watches.map(kv => (kv._1, kv._2.clone()))
  }

  /** Records a new active constraint and adds its variables to the index */
  private def record(constraint: Constraint) {
    active += constraint
    for(v <- constraint.variables) {
      activeConstraintsForVar.getOrElseUpdate(v, mutable.Set()) += constraint
    }
  }

  /** Removes a constraint from the active list and removes it from the variable index */
  private def onSatisfaction(constraint: Constraint) {
    if(active.contains(constraint)) {
//      assert(!satisfied.contains(constraint), s"Constraint $constraint already recorded as satisfied")
      active -= constraint
      satisfied += constraint
      for(v <- constraint.variables) {
        assert(activeConstraintsForVar.contains(v) && activeConstraintsForVar(v).contains(constraint))
        activeConstraintsForVar(v) -= constraint
      }
    }
  }

  def addWatcher(constraint: Constraint, watcher: Constraint) {
    if(!watchers.contains(constraint)) {
      // constraint is not watched yet, record its variable
      watchers.put(constraint, mutable.Set())
      for(v <- constraint.variables)
        watchedConstraintsForVar.getOrElseUpdate(v, mutable.Set()) += constraint
      csp.addEvent(NewWatchedConstraint(constraint))
    }
    watchers(constraint) += watcher
    watches.getOrElseUpdate(watcher, mutable.Set()) += constraint
  }

  private def removeWatcher(constraint: Constraint, watcher: Constraint) {
    assert(watchers.contains(constraint))
    assert(watches.contains(watcher))
    assert(watches(watcher).contains(constraint))
    watches(watcher) -= constraint
    watchers(constraint) -= watcher
    if(watchers(constraint).isEmpty) {
      for(v <- constraint.variables)
        watchedConstraintsForVar(v) -= constraint
    }
    if(watches(watcher).isEmpty)
      watches(watcher) -= watcher
  }

  def activeWatching(variable: IVar) : Iterable[Constraint] =
    activeConstraintsForVar.getOrElse(variable, Nil)

  def monitoredWatching(variable: IVar) : Iterable[Constraint] =
    watchedConstraintsForVar.getOrElse(variable, Nil)

  def monitoring(constraint: Constraint) : Iterable[Constraint] =
    watchers.getOrElse(constraint, Nil)

  def isActive(constraint: Constraint) = active.contains(constraint)

  def isWatched(constraint: Constraint) = watchers.contains(constraint)

  def all = active ++ satisfied

  override def handleEvent(event: Event) {
    event match {
      case Satisfied(constraint) =>
        onSatisfaction(constraint)
      case NewConstraint(constraint) =>
        record(constraint)
      case _ =>
    }
  }

  override def clone(newCSP: CSP): ConstraintStore = new ConstraintStore(newCSP, Some(this))
}
