package fr.laas.fape.anml.model.ir

trait IRStatement {
  require(id.nonEmpty, "Statement has an empty ID: "+this)
  def id : String
}
