package fape.acting.messages

import fape.core.execution.model.AtomicAction
import planstack.anml.model.concrete.ActRef
import scala.collection.JavaConverters._

case class Success(a: AtomicAction)
case class Failed(a: AtomicAction)
case class Execute(a: AtomicAction)

object AAction {
  def apply(id: ActRef, name: String, params: Seq[String], start: Int, minDur: Int, maxDur: Int) = new AtomicAction(id, name, params.asJava, start, minDur, maxDur)
  def unapply(a: AtomicAction) : Option[(ActRef, String, Seq[String], Int, Int, Int)] = Some(a.id, a.name, a.params.asScala.toList, a.mStartTime, a.minDuration, a.maxDuration)
}




sealed trait ObserverMessage
object GetProblemFromScene extends ObserverMessage
case class ProblemFromScene(anml: String) extends ObserverMessage
object ErrorOnProblemGeneration extends ObserverMessage