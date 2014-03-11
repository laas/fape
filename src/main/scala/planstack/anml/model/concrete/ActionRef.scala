package planstack.anml.model.concrete

import planstack.anml.model.AnmlProblem
import planstack.anml.model.abs.AbstractActionRef


/** Reference to a concrete action, containing, the name of the action, its parameters and its ID
   * @param name Name of the abstract action
   * @param args Arguments of the action (in the form of global variables)
   * @param id Global and unique ID of the action
   */
class ActionRef(val name:String, val args:List[String], val id:String, val parentID:Option[String])


object ActionRef {

//  def apply(pb:AnmlProblem, ref:AbstractActionRef)
}