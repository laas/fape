package planstack.constraints.stn

import planstack.constraints.stn.Weight
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
 * The STN base abstract class.
 * It contains pretty much everything except the implementation of checkConsistency.
 */
abstract class STN(val g : Graph, var consistent : Boolean) extends AbstractSTN {

  def this() = this(new AdjacencyList(), true)

  def addVar() : Int = {
    if(!consistent) println("Error: adding variable %d to inconsistent STN".format(g.numVertices))

    return g.addVertex()
  }

  /**
   * Adds a constraint to the STN specifying that v2 - v1 <= w
   * If a stronger constraint is already present, the STN isn't modified
   * @param v1
   * @param v2
   * @param w
   * @return true if the STN was updated
   */
  def addConstraintFast(v1:Int, v2:Int, w:Int) : Boolean = {
    if(!consistent) {
      println("Error: adding constraint to inconsistent STN")
      return false
    }
    consistent = false
    val oldW = g.getWeight(v1, v2)
    if(oldW > new Weight(w)) {
      g.setEdge(v1, v2, w)
      return true
    } else {
      return false
    }
  }

  def addConstraint(v1:Int, v2:Int, w:Int) : Boolean = {
    addConstraintFast(v1, v2, w)
    checkConsistency()
  }

  def checkConsistency() : Boolean

  override def clone() : STN = { throw new Exception("Clone in STN is abstract") }
}






