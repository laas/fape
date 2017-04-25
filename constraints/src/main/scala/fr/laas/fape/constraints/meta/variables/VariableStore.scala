package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.BooleanDomain
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}

import scala.collection.mutable

object VariableStore {
  var nextID : Int = 0
}

class VariableStore(csp: CSP, toClone: Option[VariableStore] = None) {

  import VariableStore._

  val varsByRef = mutable.Map[Any, IntVariable]()
  val timepointsByRef = mutable.Map[Any, Timepoint]()
  val distanceVariables = mutable.Map[(Timepoint, Timepoint), TemporalDelay]()

  toClone match {
    case Some(base) =>
      varsByRef ++= base.varsByRef
      timepointsByRef ++= base.timepointsByRef
      distanceVariables ++= base.distanceVariables
    case None =>
  }

  def getNextVariableId() : Int = { nextID += 1; nextID-1 }

  def getBooleanVariable(ref: Any) : BooleanVariable = {
    assert(!varsByRef.contains(ref))
    varsByRef(ref).asInstanceOf[BooleanVariable]
  }

  def getReificationVariable(constraint: Constraint) : ReificationVariable = {
    if(!varsByRef.contains(constraint)) {
      val v = new ReificationVariable(new BooleanDomain(Set(true, false)), constraint)
      varsByRef.put(constraint, v)
    }
    varsByRef(constraint).asInstanceOf[ReificationVariable]
  }

  def getTimepoint() : Timepoint = {
    val tp = new Timepoint(getNextVariableId(), None)
    csp.variableAdded(tp)
    tp
  }

  def getTimepoint(ref: Any) : Timepoint = {
    if(!timepointsByRef.contains(ref)) {
      val tp = new Timepoint(getNextVariableId(), Some(ref))
      timepointsByRef.put(ref, tp)
      csp.variableAdded(tp)
    }
    timepointsByRef(ref)
  }

  def getDelayVariable(from: Timepoint, to: Timepoint) : TemporalDelay = {
    if(!distanceVariables.contains((from, to))) {
      val d = new TemporalDelay(from, to)
      distanceVariables.put((from, to), d)
    }
    distanceVariables((from, to))
  }

  def getVariable(ref: Any) : IntVariable = {
    assert(varsByRef.contains(ref))
    varsByRef(ref)
  }

  def hasVariableForRef(ref: Any) = varsByRef.contains(ref)

  def setVariableForRef(ref: Any, variable: IntVariable): Unit = {
    assert(!hasVariableForRef(ref))
    varsByRef.put(ref, variable)
  }

  def clone(newCSP: CSP) : VariableStore = new VariableStore(newCSP, Some(this))
}
