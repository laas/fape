package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.{Constraint, ReificationConstraint}
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.IVar

import scala.collection.mutable

class ConstraintStore(_csp: CSP, toClone: Option[ConstraintStore]) {
  implicit val csp = _csp


  def this(_csp: CSP) = this(_csp, None)

  val active: mutable.ArrayBuffer[Constraint] = toClone match {
    case None => mutable.ArrayBuffer[Constraint]()
    case Some(base) => base.active.clone()
  }

  val satisfied: mutable.ArrayBuffer[Constraint] = toClone match {
    case None => mutable.ArrayBuffer[Constraint]()
    case Some(base) => base.satisfied.clone()
  }

  private val activeConstraintsForVar: mutable.Map[IVar, mutable.ArrayBuffer[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.activeConstraintsForVar.map(kv => (kv._1, kv._2.clone()))
  }

  private val watchedConstraintsForVar: mutable.Map[IVar, mutable.ArrayBuffer[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watchedConstraintsForVar.map(kv => (kv._1, kv._2.clone()))
  }

  private val watchers: mutable.Map[Constraint, mutable.ArrayBuffer[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watchers.map(kv => (kv._1, kv._2.clone()))
  }

  private val watches: mutable.Map[Constraint, mutable.ArrayBuffer[Constraint]] = toClone match {
    case None => mutable.Map()
    case Some(base) => base.watches.map(kv => (kv._1, kv._2.clone()))
  }

  /** Records a new active constraint and adds its variables to the index */
  private def record(constraint: Constraint) {
    active += constraint
    for(v <- constraint.variables) {
      activeConstraintsForVar.getOrElseUpdate(v, mutable.ArrayBuffer()) += constraint
    }
  }

  /** Removes a constraint from the active list and removes it from the variable index */
  private def onSatisfaction(constraint: Constraint) {
    if(active.contains(constraint)) {
      active -= constraint
      satisfied += constraint
      for(v <- constraint.variables) {
        assert(activeConstraintsForVar.contains(v) && activeConstraintsForVar(v).contains(constraint))
        activeConstraintsForVar(v) -= constraint
        if(activeConstraintsForVar(v).isEmpty)
          activeConstraintsForVar -= v
      }
    }
    for(watched <- monitoredBy(constraint).toList)
      removeWatcher(watched, constraint)
  }

  def addWatcher(constraint: Constraint, watcher: Constraint) {
    if(!watchers.contains(constraint)) {
      // constraint is not watched yet, record its variable and notify other components
      watchers.put(constraint, mutable.ArrayBuffer())
      for(v <- constraint.variables)
        watchedConstraintsForVar.getOrElseUpdate(v, mutable.ArrayBuffer()) += constraint
      csp.addEvent(WatchConstraint(constraint))
      constraint.onWatch
    }
    watchers(constraint) += watcher
    watches.getOrElseUpdate(watcher, mutable.ArrayBuffer()) += constraint
  }

  private def removeWatcher(constraint: Constraint, watcher: Constraint) {
    assert1(watchers.contains(constraint))
    assert1(watches.contains(watcher))
    assert1(watches(watcher).contains(constraint))
    watches(watcher) -= constraint
    watchers(constraint) -= watcher
    if(watchers(constraint).isEmpty) {
      // nobody is watching it anymore, remove variable watches and notify other components
      for(v <- constraint.variables) {
        watchedConstraintsForVar(v) -= constraint
        if(watchedConstraintsForVar(v).isEmpty)
          watchedConstraintsForVar -= v
      }
      watchers -= constraint
      csp.addEvent(UnwatchConstraint(constraint))
    }
    if(watches(watcher).isEmpty)
      watches -= watcher
  }

  def activeWatching(variable: IVar) : Iterable[Constraint] =
    activeConstraintsForVar.getOrElse(variable, Nil)

  def monitoredWatching(variable: IVar) : Iterable[Constraint] =
    watchedConstraintsForVar.getOrElse(variable, Nil)

  /** All constraints monitoring "constraint" */
  def monitoring(constraint: Constraint) : Iterable[Constraint] =
    watchers.getOrElse(constraint, Nil)

  /** All constraints monitored by "constraint" */
  def monitoredBy(constraint: Constraint) : Iterable[Constraint] =
    watches.getOrElse(constraint, Nil)

  def isActive(constraint: Constraint) = active.contains(constraint)

  def isWatched(constraint: Constraint) = watchers.contains(constraint)

  /** All constraints that have been posted (not including the ones that are watched) */
  def all = active ++ satisfied

  /** All constraints that are currently being monitored */
  def watched = watchers.keys

  /** Handle events that should be handled before all other components,
    * mainly to record new constraints. */
  def handleEventFirst(event: Event) {
    event match {
      case Satisfied(constraint) =>
        onSatisfaction(constraint)
      case NewConstraint(constraint) =>
        record(constraint)
      case _ =>
    }
  }

  /** Handle events that should be handled after all other components,
    * mainly to clean up on satisfaction of watch constraints. */
  def handleEventLast(event: Event) {
    event match {
      case e: WatchedSatisfactionUpdate =>
        if(e.constraint.watched)
          for(watcher <- monitoring(e.constraint).toList) // defensive copy as the list will be modified
            removeWatcher(e.constraint, watcher)
        assert1(!e.constraint.watched)
      case UnwatchConstraint(c)  =>
        // constraint is not watched anymore, remove all remove all subwatches of this constraint
        for(watched <- monitoredBy(c))
          removeWatcher(watched, c)
      case _ =>
    }
  }

  def clone(newCSP: CSP): ConstraintStore = new ConstraintStore(newCSP, Some(this))
}
