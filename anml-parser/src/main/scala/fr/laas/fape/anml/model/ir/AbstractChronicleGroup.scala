package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.abs.AbstractChronicle
import fr.laas.fape.anml.model.abs.statements.AbstractStatement

abstract class AbstractChronicleGroup {
  def firsts : List[AbstractStatement]
  def lasts : List[AbstractStatement]
  def statements : List[AbstractStatement]
  def chronicle : AbstractChronicle


}
