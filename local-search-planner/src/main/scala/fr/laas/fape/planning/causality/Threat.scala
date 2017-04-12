package fr.laas.fape.planning.causality

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, DisjunctiveConstraint, Tautology}
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.IVar
import fr.laas.fape.planning.structures.{Change, Holds, PStruct}

object Threat {

  def apply(c1: Change, c2: Change) : Constraint = {
    val disjuncts = List(c1.sv =!= c2.sv, c1.persists.end <= c2.changing.start, c2.persists.end <= c1.changing.start)
    new Threat(disjuncts, c1, c2)
  }

  def apply(c: Change, h: Holds) : Constraint = {
    val disjuncts = List(
      c.sv =!= h.sv, // only applicable if on the same state variable
      h.persists.end <= c.changing.start, // holds is before the changing period
      c.value === h.value && c.persists.start <= h.persists.start && h.persists.end <= c.persists.end, // holds in contained in persitence period
      c.persists.end < h.persists.start) // holds is after the persistence

    new Threat(disjuncts, c, h)
  }

  def apply(h1: Holds, h2: Holds) : Constraint = {
    val disjuncts = List(h1.sv =!= h2.sv, h1.value === h2.value, h1.persists.end < h2.persists.start, h2.persists.end <= h1.persists.start)
    new Threat(disjuncts, h1,  h2)
  }
}

/** A threat is a disjunctive constraint that enforce two planning structures to be non conflicting */
class Threat(disjuncts: Seq[Constraint], s1: PStruct, s2: PStruct)
  extends DisjunctiveConstraint(disjuncts) {

  override def toString =  s"threat($s1, $s2): ${super.toString}"
}