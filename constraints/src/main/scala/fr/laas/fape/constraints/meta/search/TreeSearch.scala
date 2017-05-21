package fr.laas.fape.constraints.meta.search

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.decisions.DecisionOption

import scala.collection.mutable

object NoSolution extends Enumeration {
  type Status = Value
  val NO_SOLUTION, NO_SOLUTION_BELOW_MAX_DEPTH = Value
}

class TreeSearch(nodes: Seq[CSP]) {

  private var numExpansions = 0
  private var numAppliedDecisions = 0

  private var queue = mutable.PriorityQueue[CSP]()
  nodes.foreach(n => queue.enqueue(n))

  def incrementalDeepeningSearch(maxDepth: Int = Integer.MAX_VALUE) : Either[CSP, NoSolution.Status] = {
    val startTimeMs = System.currentTimeMillis()
    val backupQueue = queue.clone()
    for(i <- 0 to maxDepth) {
      queue = backupQueue.clone()
      search(i) match {
        case Left(solution) =>
          println(s"Solution found in ${System.currentTimeMillis()-startTimeMs}ms with $numExpansions/$numAppliedDecisions expansions/decisions up to depth $i.")
          return Left(solution)
        case Right(NoSolution.NO_SOLUTION) => return Right(NoSolution.NO_SOLUTION)
        case Right(NoSolution.NO_SOLUTION_BELOW_MAX_DEPTH) => // continue
      }
      if(i == maxDepth)
        return Right(NoSolution.NO_SOLUTION_BELOW_MAX_DEPTH)
    }

    println(s"No solution found after $numExpansions expansions (in ${System.currentTimeMillis()-startTimeMs}ms)")
    Right(NoSolution.NO_SOLUTION)
  }

  private def applyTrivialDecisions(_csp: CSP, maxDecisionsToApply: Int) : CSP = {
    implicit val csp = _csp
    csp.propagate()
    if(maxDecisionsToApply == 0)
      return csp
    for(decision <- csp.decisions.pending if decision.pending) {
      if(decision.numOption == 0)
        throw new InconsistentBindingConstraintNetwork()
      else if(decision.numOption == 1) {
        numAppliedDecisions += 1
        decision.options.head.enforceIn(csp)
        return applyTrivialDecisions(csp, maxDecisionsToApply -1)
      }
    }
    csp
  }

  def search(maxDepth: Int = Integer.MAX_VALUE): Either[CSP, NoSolution.Status] = {
    var maxDepthReached = false
    while(queue.nonEmpty) {

      implicit val csp = queue.dequeue()
//      println(" "*cur.depth + "X" + " "*(maxDepth-cur.depth-1)+"|")
      numExpansions += 1
      try {
        applyTrivialDecisions(csp, 50)
        csp.propagate()

        // variables by increasing domain size
        val decisions = csp.decisions.pending
          .filter(_.pending)
          .sortBy(_.numOption)

        // no decision left, success!
        if(decisions.isEmpty) {
          println(s"Got solution of makespan: " + csp.makespan)
          return Left(csp)
        }

         val decision = decisions.head

        def apply(csp: CSP, decision: DecisionOption) : Option[CSP] = {
          try {
            decision.enforceIn(csp)
            csp.propagate()
            Some(csp)
          } catch {
            case e: InconsistentBindingConstraintNetwork =>
              None
          }
        }

        val children = decision.options.flatMap(opt => apply(csp.clone, opt))
        numAppliedDecisions += children.size
        for(x <- children)
          if(x.depth <= maxDepth)
            queue.enqueue(x)
          else
            maxDepthReached = true

      } catch {
        case e: InconsistentBindingConstraintNetwork =>
          // inconsistent node, go to next
      }
    }

    if(maxDepthReached)
      Right(NoSolution.NO_SOLUTION_BELOW_MAX_DEPTH)
    else
      Right(NoSolution.NO_SOLUTION)
  }

}
