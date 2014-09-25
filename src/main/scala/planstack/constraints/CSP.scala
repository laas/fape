package planstack.constraints

import planstack.constraints.bindings.{ConservativeConstraintNetwork, IntBindingListener}
import planstack.constraints.stn.STNManager

/**
 *
 * @param bindings A binding constraints network.
 * @param stn A simple temporal network.
 * @param varsToConstraints Mixed bindings/STN constraints that have not been propagated yet.
 * @tparam VarRef Type of the variables in the binding constraint network.
 * @tparam TPRef Type of the time points in the STN
 * @tparam ID Type of identifiers for constraints in the STN.
 */
class CSP[VarRef, TPRef, ID](
                          val bindings : ConservativeConstraintNetwork[VarRef],
                          val stn : STNManager[TPRef,ID],
                          protected[constraints] var varsToConstraints : Map[VarRef, List[Tuple4[TPRef,TPRef,Option[ID],(Int=>Int)]]]
                          )
  extends IntBindingListener[VarRef]
{
  bindings.setListener(this)

  def this() = this(new ConservativeConstraintNetwork[VarRef](), new STNManager[TPRef,ID](), Map())

  def this(toCopy : CSP[VarRef,TPRef,ID]) = this(toCopy.bindings.DeepCopy(), toCopy.stn.DeepCopy(), toCopy.varsToConstraints)

  def futureConstraint(u:TPRef,v:TPRef,id:ID,f:(Int=>Int)) =
    Tuple4[TPRef,TPRef,ID,(Int=>Int)](u, v, id, f)

  /** Add a constraint u < v + d */
  def addMaxDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, Tuple4(u,v,None,(x:Int)=>x) :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(Tuple4(u,v,None,(x:Int)=>x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  /** Add a constraint u < v + d */
  def addMaxDelayWithID(u:TPRef, v:TPRef, d:VarRef, id:ID): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, Tuple4(u,v,Some(id),(x:Int)=>x) :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(Tuple4(u,v,Some(id),(x:Int)=>x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def addMinDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, Tuple4(v,u,None,(x:Int) => -x) :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(Tuple4(v,u,None,(x:Int) => -x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def addMinDelayWithID(u:TPRef, v:TPRef, d:VarRef, id:ID): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, Tuple4(v,u,Some(id),(x:Int) => -x) :: varsToConstraints(d))
      else
        varsToConstraints.updated(d, List(Tuple4(v,u,Some(id),(x:Int) => -x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def removeConstraintsWithID(id:ID) = {
    // remove all pending cosntraints with this ID
    for((k,v) <- varsToConstraints) {
      // todo: move to scala 2.11 and use contains
      varsToConstraints = varsToConstraints.updated(k, v.filter(tup => !tup._3.exists(candidate => candidate == id)))
    }
    stn.removeConstraintsWithID(id)
  }

  override def onBinded(variable: VarRef, value: Int): Unit = {
    if(varsToConstraints contains variable) {
      for ((u, v, optID, f) <- varsToConstraints(variable)) {
        optID match {
          case Some(id) => stn.EnforceMaxDelayWithID(u, v, f(value), id)
          case None => stn.EnforceMaxDelay(u, v, f(value))
        }
      }
      // remove the entries of this variable from the table
      varsToConstraints = varsToConstraints - variable
    }
  }
}
