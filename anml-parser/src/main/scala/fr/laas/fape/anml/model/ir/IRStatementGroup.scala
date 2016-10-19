package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle

abstract class IRStatementGroup {
  def firsts : List[IRStatement]
  def lasts : List[IRStatement]
  def statements : List[IRStatement]
  def process(f : (IRStatement => AbstractChronicle)) : AbstractChronicleGroup
}
