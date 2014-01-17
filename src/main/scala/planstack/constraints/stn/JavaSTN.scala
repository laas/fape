package planstack.constraints.stn

class JavaSTN(val backend:FilipSTN) extends AbstractSTN {

  if(FilipSTN.precalc == null)
    FilipSTN.precalc_inic()

  def this() = this(new FilipSTN())

  def addVar(): Int = backend.add_v()

  def addConstraint(v1: Int, v2: Int, w: Int): Boolean = {
    if(backend.edge_consistent(v1, v2, FilipSTN.inf, w)) {
      backend.propagate(v1, v2, FilipSTN.inf, w)
      return true
    } else {
      println("STN is inconsistent %d %d %d".format(v1, v2, w))
      return false
    }
  }

  override def clone() : JavaSTN = new JavaSTN(new FilipSTN(backend))
}
