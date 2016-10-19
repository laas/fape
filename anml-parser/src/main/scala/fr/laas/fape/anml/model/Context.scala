package fr.laas.fape.anml.model

import fr.laas.fape.anml.model.concrete.{InstanceRef, TemporalInterval}
import fr.laas.fape.anml.model.ir.{IRSimpleVar, IRVar}

/** A context where all references are fully defined (i.e. every local reference has a corresponding global reference).
  *
  * {{{
  *   // Definition of the action
  *   action Move(Location a, Location b) {
  *     ...
  *   };
  *
  *   // Reference of the action, where LA is an instance of type Location
  *   Move(LA, any_)
  * }}}
  *
  * The previous example would give an [[Action]] with the following Context:
  *
  *  - parentContext: `Some(anmlProblem)`
  *
  *  - variables : `{ a -> (Location, LA, b -> (Location, any_ }`
  *
  *  - actions: {}
  *
  *  - varsToCreate: `{(Location, any_)}`
  *
  * @param parentContext An optional parent context. If given it has to be a [[Context]] (ie fully defined).
  */
class Context(
    pb:AnmlProblem,
    val label: String,
    val parentContext:Option[Context],
    val interval: TemporalInterval)
  extends AbstractContext(pb) {

    override def addUndefinedVar(v: IRSimpleVar): Unit = {
      assert(!variables.contains(v), "Local variable already defined: "+v)
      assert(!nameToLocalVar.contains(v.name), "Local variable already recorded: "+v)
      nameToLocalVar. += ((v.name, v))
    }

    def bindVarToConstant(name:LVarRef, const:InstanceRef): Unit = {
      assert(variables.contains(name))
      val previousGlobal = variables(name)
      variables.put(name, const)
    }
  }
