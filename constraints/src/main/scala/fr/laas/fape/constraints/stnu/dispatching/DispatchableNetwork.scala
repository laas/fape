package fr.laas.fape.constraints.stnu.dispatching

import scala.collection.JavaConverters._
import scala.collection.mutable
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.pending.IntExpression
import fr.laas.fape.constraints.stnu.morris.DCMorris.{Lower, Req, Upper}
import fr.laas.fape.constraints.stnu.{InconsistentTemporalNetwork, STNU}
import fr.laas.fape.constraints.stnu.morris.{DCMorris, TemporalNetwork}
import fr.laas.fape.constraints.stnu.structurals.{DistanceMatrix, StnWithStructurals}
import planstack.structures.{IList, ISet}

class WaitConstraint(val src: TPRef, val dst: TPRef, val dist: Int, val label: TPRef) extends TemporalConstraint {
  override def usedVariables: Set[Variable] = Set(src, dst, label)
}

class DispatchableNetwork[ID](val stn: StnWithStructurals[ID]) {
  import DistanceMatrix.plus

  // disable pseudo controllability checking as execution will provide updated information on the occurrence of
  // contingent points
  stn.shouldCheckPseudoControllability = false

  assert(stn.getStartTimePoint.nonEmpty, "A dispatchable STNU must have a temporal origin")

  /** Recorded WaitConstraints index by their sources and labels*/
  private val waitsBySource = mutable.Map[TPRef,mutable.ArrayBuffer[WaitConstraint]]()
  private val waitsByLabel  = mutable.Map[TPRef,mutable.ArrayBuffer[WaitConstraint]]()

  // record callback to propagate wait constraints on all updates of earliest start times
  stn.addEarliestExecutionUpdateListener(tp => {
    for(wait <- waitsByLabel.getOrElse(tp, Nil) ++ waitsBySource.getOrElse(tp, Nil))
      enforceWait(wait)
  })

  def addConstraint(constraint: TemporalConstraint): Unit = {
    constraint match {
      case c: MinDelayConstraint =>
        stn.addConstraint(c)
      case c: WaitConstraint =>
        waitsBySource.getOrElseUpdate(c.src, { mutable.ArrayBuffer() }) += c
        waitsByLabel.getOrElseUpdate(c.label, { mutable.ArrayBuffer() }) += c
        enforceWait(c)
      case c: ContingentConstraint =>
        stn.addMinDelay(c.src, c.dst, c.min.get)
        stn.addMaxDelay(c.src, c.dst, c.max.get)
    }
  }

  /** Adds a new minDelay constraint to ensure the given wait constraint is respected for the current earliest times */
  private def enforceWait(wait: WaitConstraint): Unit = {
    val est = Math.min(plus(stn.getEarliestTime(wait.src), wait.dist), stn.getEarliestTime(wait.label))
    stn.addMinDelay(stn.getStartTimePoint.get, wait.dst, est)
  }

  /** All timepoints that have been marked as executed */
  private val executions = mutable.Map[TPRef,Int]()

  /** Returns true if the given timepoint has previously been marked as executed */
  def isExecuted(tp: TPRef) = executions.contains(tp)

  /** Mark the timepoint as executed at hte given time */
  def setExecuted(tp: TPRef, time: Int): Unit = {
    executions += ((tp, time))
    stn.setTime(tp, time)
  }

  /** Pushes back all non-executed timepoints to be after the given time */
  def setCurrentTime(time: Int): Unit = {
    for(tp <- stn.timepoints.asScala if !executions.contains(tp)) {
      if(tp.genre.isDispatchable) {
        stn.addMinDelay(stn.start.get, tp, time)
      } else if(tp.genre.isContingent) {
        if (stn.getLatestTime(tp) <= time)
          stn.addMinDelay(stn.start.get, tp, time)
        else
          setExecuted(tp, stn.getLatestTime(tp))
      }
    }
  }

  /** Returns all timepoints that are executable for the given current time. */
  def getExecutables(currentTime: Int): IList[TPRef] = {
    setCurrentTime(currentTime)
    // executables are all dispatchable that can be executed at the current time
    val executables = stn.timepoints.asScala
      .filter(_.genre.isDispatchable)
      .filter(stn.getEarliestTime(_) == currentTime)

    // timepoints that are not executed yet
    val unexecutedPredecessors =
      (executables ++ stn.contingentLinks.map(c => c.dst)).filterNot(isExecuted(_))

    // restrict executables to timepoints with not predecessor that is not executed yet
    val executablesWithNoExecutablePredecessors =
      executables.filter(tp => unexecutedPredecessors.forall(pred =>
        if(stn.getMinDelay(pred, tp) == 0)
          stn.getMaxDelay(pred, tp) == 0 // only executable if the two timepoints must be concurrent
        else
          true
      ))
    new IList[TPRef](executablesWithNoExecutablePredecessors)
  }
}

object DispatchableNetwork {

  def getDispatchableNetwork[ID](stn: STNU[ID], necessarilyObservableTimepoints: java.util.Set[TPRef]) : DispatchableNetwork[ID] =
    getDispatchableNetwork(stn, necessarilyObservableTimepoints.asScala.toSet)

  def getDispatchableNetwork[ID](stn: STNU[ID], necessarilyObservableTimepoints: Set[TPRef]) : DispatchableNetwork[ID] = {
    // build network for DC checking
    val tn = TemporalNetwork.build(stn.constraints.asScala, necessarilyObservableTimepoints, Set())
      .withoutInvisible
      .normalForm

    // check Dynamic Controllability
    val morris = new DCMorris()
    for(e <- tn.constraints)
      morris.addEdge(e)
    if(!morris.determineDC()._1)
      throw new InconsistentTemporalNetwork("Temporal network is not Dynamically Controllable")

    val timepointsFromIDs = stn.timepoints.asScala
      .filter(!_.genre.isStructural)
      .map(tp => (tp.id, tp))
      .toMap
    def tp(id: Int) = timepointsFromIDs(id)
    import IntExpression.lit

    // build the dispatchable network by extending the source STNU with all constraints infered by DC-Morris
    val dispatchableNetwork = new DispatchableNetwork(stn.asInstanceOf[StnWithStructurals[ID]])
    for(e <- morris.edges ++ morris.edgesForDispatchability if timepointsFromIDs.contains(e.from) && timepointsFromIDs.contains(e.to)) {
      e match {
        case Req(src, dst, d, _) =>
          dispatchableNetwork.addConstraint(new MinDelayConstraint(tp(dst), tp(src), lit(-d)))
        case Upper(src, dst, dist, label, _) =>
          dispatchableNetwork.addConstraint(new WaitConstraint(tp(dst), tp(src), -dist, tp(label)))
        case x:Lower =>
      }
    }
    dispatchableNetwork
  }
}
