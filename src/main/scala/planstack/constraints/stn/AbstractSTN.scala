package planstack.constraints.stn

trait AbstractSTN {
  def addVar() : Int
  def addConstraint(v1:Int, v2:Int, w:Int) : Boolean
//  def checkConsistency() : Boolean
  override def clone() : AbstractSTN = { throw new Exception("Clone is STN is abstract") }
}
