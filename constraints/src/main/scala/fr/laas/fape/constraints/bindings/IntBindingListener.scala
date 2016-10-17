package fr.laas.fape.constraints.bindings

trait IntBindingListener[VarRef] {

  /** Invoked when an integer variable is binded in a constraint network.
    *
    * This instance must have been recorded as a listener in the corresponding [[ConservativeConstraintNetwork]] first.
    * @param variable Integer variable that was binded.
    * @param value Value of the variable.
    */
  def onBinded(variable : VarRef, value : Int)
}
