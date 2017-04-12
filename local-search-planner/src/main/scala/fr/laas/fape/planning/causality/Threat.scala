package fr.laas.fape.planning.causality

import fr.laas.fape.constraints.meta.constraints.{Constraint, DisjunctiveConstraint, Tautology}
import fr.laas.fape.planning.structures.{Change, Holds}

object Threat {

  def apply(c1: Change, c2: Change) : Constraint = {
    c1.sv =!= c2.sv || c1.persists.end <= c2.changing.start || c2.persists.end <= c1.changing.start
  }

  def apply(c: Change, h: Holds) : Constraint = {
    c.sv =!= h.sv || ( // only applicable if on the same state variable
      h.persists.end <= c.changing.start || // holds is before the changing period
      c.persists.start <= h.persists.start && h.persists.end <= c.persists.end || // holds in contained in persitence period
      c.persists.end < c.persists.start // holds is after the persistence
      )
  }

  def apply(h1: Holds, h2: Holds) : Constraint =
    h1.sv =!= h2.sv || h1.value === h2.value || h1.persists.end < h2.persists.start || h2.persists.end <= h1.persists.start


}
