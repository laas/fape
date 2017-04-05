package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}

import scala.collection.mutable

class VariableStore(csp: CSP, toClone: Option[VariableStore] = None) {

  private var nextID : Int = 0

  val varsByRef = mutable.Map[Any, Variable]()
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

  def getVariable(ref: Option[Any] = None) : Variable = new Variable(getNextVariableId(), ref)

  def getBooleanVariable(ref: Option[Any] = None) : BooleanVariable = ref match {
    case None => new BooleanVariable(getNextVariableId(), None)
    case Some(x) =>
      if(!varsByRef.contains(x))
        varsByRef.put(x, new BooleanVariable(getNextVariableId(), ref))
      varsByRef(x).asInstanceOf[BooleanVariable]
  }

  def getReificationVariable(constraint: Constraint) : ReificationVariable = {
    if(!varsByRef.contains(constraint)) {
      val v = new ReificationVariable(getNextVariableId(), constraint)
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
      val d = new TemporalDelay(from, to, getNextVariableId())
      distanceVariables.put((from, to), d)
    }
    distanceVariables((from, to))
  }

  def getVariableForRef(ref: Any) = {
    if(varsByRef.contains(ref))
      varsByRef(ref)
    else
      varsByRef.getOrElseUpdate(ref, new Variable(getNextVariableId(), Some(ref)))
  }

  def hasVariableForRef(ref: Any) = varsByRef.contains(ref)

  def setVariableForRef(ref: Any, variable: Variable): Unit = {
    assert(!hasVariableForRef(ref))
    varsByRef.put(ref, variable)
  }

  def clone(newCSP: CSP) : VariableStore = new VariableStore(newCSP, Some(this))
}
