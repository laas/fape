package planstack

import planstack.anml.pending.{IntExpression, IntLiteral, IntExpression$}

import IntExpression._

class FullSTN[AbsTP](timepointList: Seq[AbsTP]) {

  case class AnchoredTimepointDefinition(timepoint: AbsTP, anchor: AbsTP, delay: Int)
  case class STNLikeConstraint(src: AbsTP, dst: AbsTP, label: IntExpression)

  type Result = List[(AbsTP,List[AbsTP])]

  implicit def toIndex(tp:AbsTP) : Int = tpIndexes(tp)

  val tpIndexes = timepointList.zipWithIndex.toMap
  val tps : Seq[AbsTP] = timepointList
  val size = tps.length

  val INF: IntExpression = IntExpression.lit((Int.MaxValue /2)-1) // set to avoid overflow on addition of int values
  val NIL: IntExpression = IntExpression.lit(0)

  val dist :Array[Array[IntExpression]] = (for (i <- 0 until size) yield Array.fill(size)(INF)).toArray[Array[IntExpression]]

  for(i <- 0 until size)
    dist(i)(i) = NIL

  def addMinDelay(from:AbsTP, to:AbsTP, minDelay:Int) =
    addEdge(to, from, IntExpression.lit(-minDelay))

  def addMinDelay(from:AbsTP, to:AbsTP, minDelay:IntExpression) =
    addEdge(to, from, minus(minDelay))

  def addEdge(src:AbsTP, dst :AbsTP, t:IntExpression): Unit = {
    dist(src)(dst) = min(dist(src)(dst), t)
  }

  def concurrent(tp1: AbsTP, tp2: AbsTP) = equals(dist(tp1)(tp2), dist(tp2)(tp1))

  def minDelay(from: AbsTP, to:AbsTP) = minus(dist(to)(from))
  def maxDelay(from: AbsTP, to: AbsTP) = dist(from)(to)
  def beforeOrConcurrent(first: AbsTP, second: AbsTP) = lesserEqual(dist(second)(first), NIL)
  def strictlyBefore(first: AbsTP, second: AbsTP) = lesserThan(dist(second)(first), NIL)
  def between(tp: AbsTP, min:AbsTP, max:AbsTP) = beforeOrConcurrent(min, tp) && beforeOrConcurrent(tp, max)
  def strictlyBetween(tp: AbsTP, min:AbsTP, max:AbsTP) = strictlyBefore(min, tp) && strictlyBefore(tp, max)

  def floydWarshall(): Unit = {
    for(k <- 0 until size) {
      for(i <- 0 until size) {
        for(j <- 0 until size) {
          dist(i)(j) = min(dist(i)(j), sum(dist(i)(k), dist(k)(j)))
          val l = sum(dist(i)(j), dist(j)(i))
          assert(l.ub >= 0,  "Error: temporal inconsistency in the definition of this action")
          if(l.ub == 0 && !l.isKnown) {
            dist(i)(j) = IntExpression.lit(dist(i)(j).ub)
            dist(j)(i) = IntExpression.lit(dist(j)(i).ub)
          }
        }
      }
    }
  }

  private def extractFlex(prio: List[AbsTP], pending:Set[AbsTP], result: Result) : Result = {
    prio match {
      case cur::tail if pending.contains(cur) => // next priority is in pending remove it with all timepoints rigidly fixed to it
        val rigids = pending
          .filter(tp => cur != tp
            && dist(cur)(tp).isKnown && dist(tp)(cur).isKnown
            && dist(cur)(tp).get == -dist(tp)(cur).get)
          .toList
        extractFlex(tail, (pending -- rigids) - cur, (cur, rigids) :: result)

      case _::tail => // next priority item not in pending, skip it
        extractFlex(tail, pending, result)

      case Nil if pending.nonEmpty => // select one timepoint in pending to be our next flexible
        val cur = pending.head
        val rigids = pending
          .filter(tp => cur != tp
            && dist(cur)(tp).isKnown && dist(tp)(cur).isKnown
            && dist(cur)(tp).get == -dist(tp)(cur).get)
          .toList
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
    *
    * @param priorityForFlexibleTimepoints Those time points will be the first to be considered to enter the set of flexible
   */
  def minimalRepresentation(priorityForFlexibleTimepoints: List[AbsTP]) : (List[AbsTP], List[STNLikeConstraint], List[AnchoredTimepointDefinition]) = {
    floydWarshall()
    val res = extractFlex(priorityForFlexibleTimepoints, tps.toSet, Nil)
    val flexibleTps = res.map(_._1)
    val rigidTps = res.foldLeft[List[AbsTP]](Nil)((allRigids, flexRigidPair) => allRigids ++ flexRigidPair._2)

    val delays : List[STNLikeConstraint] =
      for(flex1 <- flexibleTps ; flex2 <- flexibleTps ; if flex1 != flex2 && dist(flex1)(flex2).lb < INF.get)
        yield STNLikeConstraint(flex1, flex2, dist(flex1)(flex2))

    val rigids =
      for((flex, rigids) <- res ; rigid <- rigids)
        yield AnchoredTimepointDefinition(rigid, flex, dist(rigid)(flex).get)

    (flexibleTps, delays, rigids)
  }
}
