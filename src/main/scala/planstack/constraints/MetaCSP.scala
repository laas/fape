package planstack.constraints

import planstack.constraints.bindings.{ConservativeConstraintNetwork, IntBindingListener}
import planstack.constraints.stn.STNManager
import planstack.constraints.stnu.{STNUManager, PseudoSTNUManager, MinimalSTNUManager, GenSTNUManager}

abstract class PendingConstraint[VarRef, TPRef, ID](val from:TPRef, val to:TPRef, val optID:Option[ID]) {
  def hasID(id:ID) = optID match {
    case Some(myID) => id == myID
    case None => false
  }
}

class PendingContingency[VarRef, TPRef, ID](from:TPRef, to:TPRef, optID:Option[ID], val min:VarRef, val max:VarRef)
  extends PendingConstraint[VarRef, TPRef, ID](from, to, optID)

class PendingRequirement[VarRef, TPRef, ID](from:TPRef, to:TPRef, optID:Option[ID], val value:VarRef, val f:(Int=>Int))
  extends PendingConstraint[VarRef, TPRef, ID](from, to, optID)


/**
 *
 * @param bindings A binding constraints network.
 * @param stn A simple temporal network.
 * @param varsToConstraints Mixed bindings/STN constraints that have not been propagated yet.
 * @tparam VarRef Type of the variables in the binding constraint network.
 * @tparam TPRef Type of the time points in the STN
 * @tparam ID Type of identifiers for constraints in the STN.
 */
class MetaCSP[VarRef, TPRef, ID](
                          val bindings: ConservativeConstraintNetwork[VarRef],
                          val stn: GenSTNUManager[TPRef,ID],
                          protected[constraints] var varsToConstraints: Map[VarRef, List[PendingConstraint[VarRef,TPRef,ID]]])
  extends IntBindingListener[VarRef]
{

  bindings.setListener(this)

  def this() = this(new ConservativeConstraintNetwork[VarRef](), new MinimalSTNUManager[TPRef,ID](), Map())

  def this(toCopy : MetaCSP[VarRef,TPRef,ID]) = this(toCopy.bindings.DeepCopy(), toCopy.stn.deepCopy(), toCopy.varsToConstraints)

  def futureConstraint(u:TPRef,v:TPRef,id:ID,f:(Int=>Int)) =
    Tuple4[TPRef,TPRef,ID,(Int=>Int)](u, v, id, f)

  /** Add a constraint u < v + d */
  def addMaxDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    val pending = new PendingRequirement[VarRef,TPRef,ID](u, v, None, d, (x:Int)=>x)
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, pending :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(pending))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  /** Add a constraint u +d >= v */
  def addMaxDelayWithID(u:TPRef, v:TPRef, d:VarRef, id:ID): Unit = {
    val pending = new PendingRequirement[VarRef,TPRef,ID](u, v, Some(id), d, (x:Int)=>x)
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, pending :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(pending))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def addMinDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    val pending = new PendingRequirement[VarRef,TPRef,ID](v, u, None, d, (x:Int)=> -x)
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, pending :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(pending))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def addMinDelayWithID(u:TPRef, v:TPRef, d:VarRef, id:ID): Unit = {
    val pending = new PendingRequirement[VarRef,TPRef,ID](v, u, Some(id), d, (x:Int)=> -x)
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, pending :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(pending))

    if(bindings.domainSize(d) == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def removeConstraintsWithID(id:ID) = {
    // remove all pending constraints with this ID
    for((k,v) <- varsToConstraints) {
      varsToConstraints = varsToConstraints.updated(k, v.filter(constraint => !constraint.hasID(id)))
    }
    stn.removeConstraintsWithID(id)
  }

  def addContingentConstraint(from:TPRef, to:TPRef, min:VarRef, max:VarRef): Unit = {
    val pending = new PendingContingency[VarRef,TPRef,ID](from, to, None, min, max)

    varsToConstraints += ((min, pending :: varsToConstraints.getOrElse(min, List())))
    varsToConstraints += ((max, pending :: varsToConstraints.getOrElse(max, List())))

    if(bindings.domainSize(min) == 1)
      onBinded(min, bindings.domainOfIntVar(min).get(0))
    if(bindings.domainSize(max) == 1)
      onBinded(max, bindings.domainOfIntVar(max).get(0))
  }

  def addContingentConstraintWithID(from:TPRef, to:TPRef, min:VarRef, max:VarRef, id:ID): Unit = {
    val pending = new PendingContingency[VarRef,TPRef,ID](from, to, Some(id), min, max)
    varsToConstraints += ((min, pending :: varsToConstraints.getOrElse(min, List())))
    varsToConstraints += ((max, pending :: varsToConstraints.getOrElse(max, List())))

    if(bindings.domainSize(min) == 1)
      onBinded(min, bindings.domainOfIntVar(min).get(0))
    if(bindings.domainSize(max) == 1)
      onBinded(max, bindings.domainOfIntVar(max).get(0))
  }

  /** Invoked by the binding constraint network when a variable is binded.
    * If this variable is part of a pending constraint, we propagate this one in hte STN.
    * @param variable Integer variable that was binded.
    * @param value Value of the variable.
    */
  override def onBinded(variable: VarRef, value: Int): Unit = {
    if(varsToConstraints contains variable) {
      for (c <- varsToConstraints(variable)) c match {
        case cont:PendingContingency[VarRef,TPRef,ID] => {
          // contingent constraint, add the contingent one if both variable are binded
          assert(variable == cont.max || variable == cont.min)
          val bothBinded = bindings.domainSize(cont.min) == 1 && bindings.domainSize(cont.max) == 1
          if(bothBinded) {
            // both variables are binded, we add the contingent variable
            val min = bindings.domainOfIntVar(cont.min).get(0)
            val max = bindings.domainOfIntVar(cont.max).get(0)
            cont.optID match {
              case Some(id) => stn.enforceContingentWithID(cont.from, cont.to, min, max, id)
              case None => stn.enforceContingent(cont.from, cont.to, min, max)
            }
          } else {
            // only one variable is binded, simply add a requirement
            cont.optID match {
              case Some(id) =>
                if(variable == cont.max) stn.enforceMaxDelayWithID(cont.from, cont.to, value, id)
                else stn.enforceMinDelayWithID(cont.from, cont.to, value, id)
              case None =>
                if(variable == cont.max) stn.enforceMaxDelay(cont.from, cont.to, value)
                else stn.enforceMinDelay(cont.from, cont.to, value)
            }
          }
        }
        case req:PendingRequirement[VarRef,TPRef,ID] => req.optID match {
          // requirement (only one variable), propagate it
          case Some(id) => stn.enforceMaxDelayWithID(req.from, req.to, req.f(value), id)
          case None => stn.enforceMaxDelay(req.from, req.to, req.f(value))
        }
      }
      // remove the entries of this variable from the table
      varsToConstraints = varsToConstraints - variable
    }
  }

  /** Returns true if both the binding constraint network and the STN are consistent */
  def isConsistent = bindings.isConsistent && stn.isConsistent
}
