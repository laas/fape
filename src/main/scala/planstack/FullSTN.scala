package planstack

import planstack.anml.model.abs.time.AbsTP

import scala.reflect.ClassTag


class FullSTN(timepointList: Seq[AbsTP]) {

  case class AnchoredTimepointDefinition(timepoint: AbsTP, anchor: AbsTP, delay: Int)
  case class STNLikeConstraint(src: AbsTP, dst: AbsTP, label: Int)

  type Result = List[(AbsTP,List[AbsTP])]

  implicit def toIndex(tp:AbsTP) : Int = tpIndexes(tp)

  val tpIndexes = timepointList.zipWithIndex.toMap
  val tps : Array[AbsTP] = timepointList.toArray
  val size = tps.length

  val inf = 9999999

  val dist :Array[Array[Int]] = (for (i <- 0 until size) yield Array.fill(size)(inf)).toArray[Array[Int]]

  for(i <- 0 until size)
    dist(i)(i) = 0

  def addMinDelay(from:AbsTP, to:AbsTP, minDelay:Int) =
    addEdge(to, from, -minDelay)

  def addEdge(src:AbsTP, dst :AbsTP, t:Int): Unit = {
    dist(src)(dst) = t
  }


  def floydWarshall(): Unit = {
    for(k <- 0 until size) {
      for(i <- 0 until size) {
        for(j <- 0 until size) {
          if(dist(i)(k) < inf && dist(k)(j) < inf) {
            if(dist(i)(j) > dist(i)(k) + dist(k)(j)) {
              dist(i)(j) = dist(i)(k) + dist(k)(j)
              assert(dist(i)(j) +dist(j)(i) >= 0, "Error: temporal inconsistency in the definition of this action")
            }
          }
        }
      }
    }
  }

  private def extractFlex(prio: List[AbsTP], pending:Set[AbsTP], result: Result) : Result = {
    prio match {
      case cur::tail if pending.contains(cur) => // next priority is in pending remove it with all timepoints rigidly fixed to it
        val rigids = pending.filter(tp => cur != tp && dist(cur)(tp) == -dist(tp)(cur)).toList
        extractFlex(tail, (pending -- rigids) - cur, (cur, rigids) :: result)

      case _::tail => // next priority item not in pending, skip it
        extractFlex(tail, pending, result)

      case Nil if pending.nonEmpty => // select one timepoint in pending to be our next flexible
        val cur = pending.head
        val rigids = pending.filter(tp => cur != tp && dist(cur)(tp) == -dist(tp)(cur)).toList
        extractFlex(Nil, (pending -- rigids) - cur, (cur, rigids) :: result)

      case _ =>
        assert(pending.isEmpty)
        result

    }
  }

  /**
   * Builds a summary of this particular STN by identifying timepoints that are rigidly linked to others.
   * It returns a tuple consisting of:
   *   - a list of "flexible timepoints"
   *   - a list of anchored timepoints. Those timepoints have a fixed delay with respect to flexible timepoints
   *   - a list of constraints between flexible timepoints (APSP)
   * @param priorityForFlexibleTimepoints Those time points will be the first to be considered to enter the set of flexible
   */
  def minimalRepresentation(priorityForFlexibleTimepoints: List[AbsTP]) : (List[AbsTP], List[STNLikeConstraint], List[AnchoredTimepointDefinition]) = {
    floydWarshall()
    val res = extractFlex(priorityForFlexibleTimepoints, tps.toSet, Nil)
    val flexibleTps = res.map(_._1)
    val rigidTps = res.foldLeft[List[AbsTP]](Nil)((allRigids, flexRigidPair) => allRigids ++ flexRigidPair._2)

    val delays : List[STNLikeConstraint] =
      for(flex1 <- flexibleTps ; flex2 <- flexibleTps ; if flex1 != flex2 && dist(flex1)(flex2) < inf)
        yield STNLikeConstraint(flex1, flex2, dist(flex1)(flex2))

    val rigids =
      for((flex, rigids) <- res ; rigid <- rigids)
        yield AnchoredTimepointDefinition(rigid, flex, dist(rigid)(flex))

    (flexibleTps, delays, rigids)
  }


}
