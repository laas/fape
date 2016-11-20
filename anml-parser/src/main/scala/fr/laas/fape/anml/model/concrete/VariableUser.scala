package fr.laas.fape.anml.model.concrete

import scala.collection.JavaConverters._

trait VariableUser {
  def usedVariables : Set[Variable]

  def jUsedVariables = usedVariables.asJava
}
