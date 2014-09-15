package planstack.constraints

import planstack.constraints.bindings.{IntBindingListener, ConservativeConstraintNetwork}
import planstack.constraints.stn.{STNManager, STN}

class CSP[VarRef, TPRef](
                          val bindings : ConservativeConstraintNetwork[VarRef],
                          val stn : STNManager[TPRef])
  extends IntBindingListener[VarRef]
{
  bindings.setListener(this)

  protected[constraints] var varsToConstraints : Map[VarRef, List[Tuple3[TPRef,TPRef,(Int=>Int)]]] = Map()

  def this() = this(new ConservativeConstraintNetwork[VarRef](), new STNManager[TPRef]())

  def this(toCopy : CSP[VarRef,TPRef]) = this(toCopy.bindings.DeepCopy(), toCopy.stn.DeepCopy())


  /** Add a constraint u < v + d */
  def addMaxDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, (Tuple3(u,v,(x:Int)=>x) :: varsToConstraints(d)))
      else
        varsToConstraints.updated(d, List(Tuple3(u,v,(x:Int)=>x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  def addMinDelay(u:TPRef, v:TPRef, d:VarRef): Unit = {
    varsToConstraints =
      if(varsToConstraints.contains(d))
        varsToConstraints.updated(d, (Tuple3(v,u,(x:Int) => -x) :: varsToConstraints(d)))
      else
        varsToConstraints.updated(d, List(Tuple3(v,u,(x:Int) => -x)))

    if(bindings.domainOfIntVar(d).size() == 1)
      onBinded(d, bindings.domainOfIntVar(d).get(0))
  }

  override def onBinded(variable: VarRef, value: Int): Unit = {
    if(varsToConstraints contains variable) {
      println("onBinded(%s, %s)".format(variable, value))
      for ((u, v, f) <- varsToConstraints(variable)) {
        stn.EnforceMaxDelay(u, v, f(value))
      }
      // remove the entries of this variable from the table
      varsToConstraints = varsToConstraints - variable
    }
  }
}
