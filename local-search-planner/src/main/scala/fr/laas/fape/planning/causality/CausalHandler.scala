package fr.laas.fape.planning.causality

import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.types.dynamics.{BaseDynamicType, ComposedDynamicType}
import fr.laas.fape.constraints.meta.types.statics.{BaseType}
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.planning.causality.support.SupportConstraint
import fr.laas.fape.planning.events.{PlanningEventHandler, PlanningHandler, PlanningStructureAdded}
import fr.laas.fape.planning.structures.{CausalStruct, Change, Holds}

import scala.collection.mutable

sealed trait SupportOption
case class SupportByExistingChange(c: Change) extends SupportOption
case class SupportByActionInsertion(a: ActionPotentialSupport) extends SupportOption


object DecisionPending extends SupportOption
object DecisionType extends BaseType[SupportOption]("decision-type", List((DecisionPending, 0)))

class CausalHandler(val context: PlanningHandler, base: Option[CausalHandler] = None) extends PlanningEventHandler {

  implicit val csp = context.csp

  val changes: mutable.ArrayBuffer[Change] = base match {
    case Some(prev) => prev.changes.clone()
    case None => mutable.ArrayBuffer()
  }

  val holds:  mutable.ArrayBuffer[Holds] = base match {
    case Some(prev) => prev.holds.clone()
    case None => mutable.ArrayBuffer()
  }

  val potentialSupports : PotentialSupport= base match {
    case Some(prev) => prev.potentialSupports.clone(this)
    case None => new PotentialSupport(this)
  }

  val existingSupportType: BaseDynamicType[SupportByExistingChange] = base match {
    case Some(prev) => prev.existingSupportType
    case None =>
      assert1(changes.isEmpty)
      new BaseDynamicType("existing-change-support", Nil)
  }
  val actionInsertionSupportType: BaseType[SupportByActionInsertion] = base match {
    case Some(prev) => prev.actionInsertionSupportType
    case None => new BaseType("action-insertion-support",
      potentialSupports.actionPotentialSupports.values
        .map(SupportByActionInsertion(_))
        .zipWithIndex.toList
        .map(p => (p._1, p._2+1)))
  }
  val supportType: ComposedDynamicType[SupportOption] = base match {
    case Some(prev) => prev.supportType
    case None => new ComposedDynamicType[SupportOption](DecisionType :: actionInsertionSupportType :: existingSupportType :: Nil)
  }

  def report : String = {
    def time(tp: Timepoint) : String = s"$tp:${tp.domain}"
    val sb = new StringBuilder
    sb.append("%% Changes\n")
    for(c <- changes) {
      sb.append(s"${c.sv}:=${c.value} -- change:]${time(c.changing.start)}, ${time(c.changing.end)}[ -- persist:[${time(c.persists.start)}, ${time(c.persists.end)}]\n")
    }
    sb.append("%% Holds\n")
    for(c <- holds) {
      sb.append(s"${c.sv}==${c.value} -- persist:[${time(c.persists.start)}, ${time(c.persists.end)}]\n")
    }
    sb.toString()
  }

  override def handleEvent(event: Event) {
    event match {
      case PlanningStructureAdded(s: Holds) =>
        for(h <- holds)
          csp.post(Threat(s, h))
        for(c <- changes)
          csp.post(Threat(c, s))
        csp.post(new SupportConstraint(supportType, s))
        holds += s
      case PlanningStructureAdded(s: Change) =>
        existingSupportType.addInstance(SupportByExistingChange(s), 1 + potentialSupports.actionPotentialSupports.size + changes.size)
        csp.post(s.persists.start <= s.persists.end)
        for(h <- holds)
          csp.post(Threat(s, h))
        for(c <- changes)
          csp.post(Threat(c, s))
        changes += s
      case PlanningStructureAdded(_: CausalStruct) => throw new NotImplementedError()

      case _ => // not a causal structure, ignore
    }
  }

  def clone(newContext: PlanningHandler) : CausalHandler = new CausalHandler(newContext, Some(this))
}
