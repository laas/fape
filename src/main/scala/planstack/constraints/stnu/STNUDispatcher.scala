package planstack.constraints.stnu

import planstack.structures.IList

import scala.language.implicitConversions
import ElemStatus._
import planstack.structures.Converters._
import scala.collection.JavaConverters._

class STNUDispatcher[TPRef, ID](from:GenSTNUManager[TPRef,ID]) {
  val dispatcher = new Dispatcher[ID]

  val ids : Map[TPRef,Int] =
    (for((tp, flag) <- from.timepoints) yield flag match {
      case CONTINGENT => (tp, dispatcher.addContingentVar())
      case CONTROLLABLE => (tp, dispatcher.addDispatchable())
      case _ => (tp, dispatcher.addVar())
    }).toMap

  val tps = ids.map(_.swap)

  implicit def tp2id(tp:TPRef) : Int = ids(tp)
  implicit def id2tp(id:Int) : TPRef = tps(id)

  for((from, to, d, status, optID) <- from.constraints) {
    status match {
      case CONTINGENT => optID match {
        case Some(id) => dispatcher.addContingentWithID(from, to, d, id)
        case None => dispatcher.addConstraint(from, to, d)
      }
      case _ => optID match {
        case Some(id) => dispatcher.addConstraintWithID(from, to, d, id)
        case None => dispatcher.addConstraint(from, to, d)
      }
    }
  }

  def isConsistent = dispatcher.checkConsistency()

  def setHappened(tp:TPRef): Unit = {
    dispatcher.setHappened(tp)
  }

  def getDispatchable(time:Int) : IList[TPRef] = {
    val t:Integer = time
    new IList[TPRef](dispatcher.dispatchableEvents(time).map(id => id2tp(id)).toList)
  }

  def getMaxContingentDelay(from:TPRef, to:TPRef) : Int = {
    dispatcher.edg.contingents.edge(from, to) match {
      case Some(e) => e.l.value
      case None => throw new RuntimeException("No contingent delay between those two time points")
    }
  }

  def getMinContingentDelay(from:TPRef, to:TPRef) : Int = {
    dispatcher.edg.contingents.edge(to, from) match {
      case Some(e) => -e.l.value
      case None => throw new RuntimeException("No contingent delay between those two time points")
    }
  }

}
