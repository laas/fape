package planstack.anml.model

trait VariableFactory {

  def createVar(tipe:String) : String

}

object DummyVariableFactory extends VariableFactory {

  var nextVarID = 0

  override def createVar(tipe:String) = "dv_"+{nextVarID+=1; nextVarID-1}
}
