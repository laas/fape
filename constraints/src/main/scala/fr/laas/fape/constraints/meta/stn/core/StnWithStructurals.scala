package fr.laas.fape.constraints.meta.stn.core

import fr.laas.fape.constraints.meta.stn.constraint._
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.stnu.{Controllability, InconsistentTemporalNetwork, STNU}
import fr.laas.fape.structures.IList

import scala.collection.mutable

object StnWithStructurals {
  var debugging = false

  val INF: Int = Int.MaxValue /2 -1 // set to avoid overflow on addition of int values
  val NIL: Int = 0
}

import fr.laas.fape.constraints.meta.stn.core.StnWithStructurals._

class StnWithStructurals(var nonRigidIndexes: mutable.Map[Timepoint,Int],
                         var timepointByIndex: mutable.ArrayBuffer[Timepoint],
                         var dist: DistanceMatrix,
                         var rigidRelations: RigidRelations,
                         var contingentLinks: mutable.ArrayBuffer[ContingentConstraint],
                         var optStart: Option[Timepoint],
                         var optEnd: Option[Timepoint],
                         var originalEdges: List[DistanceGraphEdge],
                         var consistent: Boolean,
                         val executed: mutable.Set[Timepoint],
                         val watchedVarsByIndex: mutable.Map[(Int,Int), mutable.ArrayBuffer[(Timepoint,Timepoint)]]
                        )
  extends DistanceMatrixListener {

  /** If true, the STNU will check that the network is Pseudo Controllable when invoking isConsistent */
  var shouldCheckPseudoControllability = true

  private var distanceChangeListener: Option[IDistanceChangeListener] = None
  def setDistanceChangeListener(listener: IDistanceChangeListener) {
    assert1(distanceChangeListener.isEmpty)
    distanceChangeListener = Some(listener)
  }

  def this() = this(mutable.Map(), mutable.ArrayBuffer(), new DistanceMatrix(), new RigidRelations(),
    mutable.ArrayBuffer(), None, None, Nil, true, mutable.Set(), mutable.Map())

  override def clone() : StnWithStructurals = new StnWithStructurals(
    nonRigidIndexes.clone(), timepointByIndex.clone(), dist.clone(), rigidRelations.clone(), contingentLinks.clone(),
    optStart, optEnd, originalEdges, consistent, executed.clone(), watchedVarsByIndex.map(kv => (kv._1, kv._2.clone()))
  )

  // make sure we are notified of any change is the distance matrix
  dist.addListener(this)

  private var _timepoints = (nonRigidIndexes.keySet ++ rigidRelations._anchorOf.keySet).toList
  def timepoints = new IList[Timepoint](_timepoints)

  private def toIndex(tp:Timepoint) : Int = nonRigidIndexes(tp)
  def timepointFromIndex(index: Int) : Timepoint = timepointByIndex(index)

  /** Returns the index of the timepoint if it is anchor or the index of its anchor otherwise */
  private def indexOfAnchor(tp: Timepoint) : Int =
    if(rigidRelations.isAnchored(tp)) toIndex(rigidRelations.anchorOf(tp))
    else toIndex(tp)

  private def isKnown(tp: Timepoint) = nonRigidIndexes.contains(tp) || rigidRelations.isAnchored(tp)

  def recordTimePoint(tp: Timepoint): Int = {
    assert1(!isKnown(tp))
    _timepoints = tp :: _timepoints
    val id = dist.createNewNode()
    nonRigidIndexes.put(tp, id)
    rigidRelations.addAnchor(tp)
    while(timepointByIndex.size <= id) {
      timepointByIndex.append(null)
    }
    assert1(timepointByIndex(id) == null)
    timepointByIndex(id) = tp
    optEnd match {
      case Some(end) => enforceMinDelay(tp, end, 0)
      case None =>
    }
    id
  }

  def forceExecutionTime(tp: Timepoint, time: Int): Unit = {
    def neighborhood(tp: Timepoint) =
      if(rigidRelations.isAnchor(tp))
        rigidRelations.getTimepointsAnchoredTo(tp).toSet
      else
        rigidRelations.getTimepointsAnchoredTo(rigidRelations.anchorOf(tp)).toSet + rigidRelations.anchorOf(tp) -tp

    try {
      this.clone().setTime(tp, time)
      // no inconsistent network exception, simply propagate those constraints (much cheaper)
      setTime(tp, time)
      // if tp is contingent, remove its incoming contingent link
      if(tp.isContingent)
        contingentLinks = contingentLinks.filter(c => !(c.dst == tp))
      executed += tp
    } catch {
      case e:InconsistentTemporalNetwork =>
        // we have conflicting constraint, remove any constraint to previously executed timepoints (directly or through anchored relations)
        val directAttached = neighborhood(tp) + tp
        val allGrounded = executed.flatMap(neighborhood(_)) ++ executed

        val newEdges = originalEdges.filter(e => {
          if(allGrounded.intersect(directAttached).nonEmpty) {
            // drop all edges to tp
            e.from != tp && e.to != tp

          } else {
            // drop all edges related the executed set and our neighborhood (which include ourselves)
            if(allGrounded.contains(e.from) && directAttached.contains(e.to)) false
            else if(directAttached.contains(e.from) && allGrounded.contains(e.to)) false
            else true
          }})
        // if tp is contingent, its incoming contingent link
        val newContingents = contingentLinks.filter(c => !(c.dst == tp))

        // remove everything
        nonRigidIndexes = mutable.Map()
        timepointByIndex = mutable.ArrayBuffer()
        dist = new DistanceMatrix()
        rigidRelations = new RigidRelations()
        contingentLinks = mutable.ArrayBuffer()
        originalEdges = List()

        // rebuild from scratch
        setTime(tp, time)
        executed += tp
        for (e <- newEdges)
          addMaxDelay(e.from, e.to, e.value)
        for (ctg <- newContingents)
          addConstraint(ctg)
    }
  }

  def addMinDelay(from:Timepoint, to:Timepoint, minDelay:Int) =
    addEdge(to, from, -minDelay)

  def addMaxDelay(from: Timepoint, to: Timepoint, maxDelay: Int) =
    addMinDelay(to, from, -maxDelay)

  private def addEdge(a:Timepoint, b :Timepoint, t:Int): Unit = {
    originalEdges = new DistanceGraphEdge(a, b, t) :: originalEdges
    if(!isKnown(a))
      recordTimePoint(a)
    if(!isKnown(b))
      recordTimePoint(b)

    val (aRef:Timepoint, aToRef:Int) =
      if(rigidRelations.isAnchored(a))
        (rigidRelations._anchorOf(a), rigidRelations.distFromAnchor(a))
      else
        (a, 0)
    val (bRef:Timepoint, refToB) =
      if(rigidRelations.isAnchored(b))
        (rigidRelations._anchorOf(b), rigidRelations.distToAnchor(b))
      else (b, 0)
    dist.enforceDist(toIndex(aRef), toIndex(bRef), DistanceMatrix.plus(DistanceMatrix.plus(aToRef, t), refToB))
  }

  def addConstraint(c: TemporalConstraint): Unit = {
    c match {
      case req: MinDelayConstraint =>
        addMinDelay(req.src, req.dst, req.minDelay)
      case cont: ContingentConstraint =>
        addMinDelay(cont.src, cont.dst, cont.min)
        addMaxDelay(cont.src, cont.dst, cont.max)
        contingentLinks.append(cont)
      case abs: AbsoluteBeforeConstraint =>
        assert1(start.nonEmpty, "Absolute constraints require a start timepoint")
        addMaxDelay(start.get, abs.tp, abs.deadline)
      case abs: AbsoluteAfterConstraint =>
        assert1(start.nonEmpty, "Absolute constraints require a start timepoint")
        addMinDelay(start.get, abs.tp, abs.deadline)
      case _ =>
        throw new RuntimeException("Constraint: "+c+" is not properly supported")
    }
  }

  /** Enforce u <= v */
  final def enforceBefore(u:Timepoint, v:Timepoint) { enforceMinDelay(u, v, 0) }

  /** Enforces u < v */
  final def enforceStrictlyBefore(u:Timepoint, v:Timepoint) { enforceMinDelay(u, v, 1) }

  /** Enforces u +d <= v */
  final def enforceMinDelay(u:Timepoint, v:Timepoint, d:Int) { addConstraint(v, u, -d) }

  /** Enforces u + d >= v */
  final def enforceMaxDelay(u:Timepoint, v:Timepoint, d:Int) { addConstraint(u, v, d) }

  /** Returns True if u can be at the same time or before v */
  final def canBeBefore(u:Timepoint, v:Timepoint) : Boolean = isConstraintPossible(v, u, 0)

  /** Returns True if u can be strictly before v */
  final def canBeStrictlyBefore(u:Timepoint, v:Timepoint) : Boolean = isConstraintPossible(v, u, -1)

  final def isDelayPossible(from:Timepoint, to:Timepoint, delay:Int) = isConstraintPossible(to, from, -delay)

  private def rigidAwareDist(a:Timepoint, b:Timepoint) : Int = {
    val (aRef:Timepoint, aToRef:Int) =
      if(rigidRelations.isAnchored(a))
        (rigidRelations._anchorOf(a), rigidRelations.distToAnchor(a))
      else
        (a, 0)
    val (bRef:Timepoint, refToB) =
      if(rigidRelations.isAnchored(b))
        (rigidRelations._anchorOf(b), rigidRelations.distFromAnchor(b))
      else (b, 0)

    val refAToRefB = distanceBetweenNonRigid(aRef, bRef)
    DistanceMatrix.plus(aToRef, DistanceMatrix.plus(refAToRefB, refToB))
  }

  private def distanceBetweenNonRigid(a: Timepoint, b: Timepoint) = {
    dist.getDistance(toIndex(a), toIndex(b))
  }

  def concurrent(tp1: Timepoint, tp2: Timepoint) = rigidAwareDist(tp1,tp2) == rigidAwareDist(tp2,tp1)

  private def minDelay(from: Timepoint, to:Timepoint) = -rigidAwareDist(to, from)
  private def maxDelay(from: Timepoint, to: Timepoint) = rigidAwareDist(from, to)
  private def beforeOrConcurrent(first: Timepoint, second: Timepoint) = rigidAwareDist(second, first) <= NIL
  private def strictlyBefore(first: Timepoint, second: Timepoint) = rigidAwareDist(second, first) < NIL
  private def between(tp: Timepoint, min:Timepoint, max:Timepoint) = beforeOrConcurrent(min, tp) && beforeOrConcurrent(tp, max)
  private def strictlyBetween(tp: Timepoint, min:Timepoint, max:Timepoint) = strictlyBefore(min, tp) && strictlyBefore(tp, max)

  /** Handler that checks STN consistency and optimizes the network upon changes in the distance matrix.
    * It also responsible for notifying the listener of the changes on watched edges. */
  override def distancesUpdated(edges: Seq[(Int, Int)]): Unit = {
    // extract watched edges whose distance was updated
    val watchesToNotify: Set[(Timepoint, Timepoint)] = // extract watches while making a defensive copy
      edges.flatMap{ case (a: Int, b:Int) => watchedVarsByIndex.getOrElse(asWatchKey(a, b), Nil) }.toSet

    // check if the network became inconsistent
    for((a, b) <- edges) {
      if(dist.getDistance(a, b) + dist.getDistance(b, a) < 0) {
        assert4(!consistentWithBellmanFord(), "BellmanFord and APSP gave different results regarding consistency")
        consistent = false
        throw new InconsistentTemporalNetwork
      }
    }
    assert4(consistentWithBellmanFord(), "BellmanFord and APSP gave different results regarding consistency")

    for((a, b) <- edges)
      checkAndProcessRigidRelation(a, b)

    // notify listeners of updated distances
    assert3(watchesToNotify.forall(p => isWatched(p._1, p._2)), "An edge became unwatched when processing the rigid relations")
    assert1(watchesToNotify.isEmpty || distanceChangeListener.nonEmpty, "Changes to notify but no recorded listener")
    for((tp1, tp2) <- watchesToNotify)
      distanceChangeListener.get.distanceUpdated(tp1, tp2)
  }

  /** Checks if the is a rigid relation between the two anchor index and merges them together if there is. */
  private def checkAndProcessRigidRelation(indexA: Int, indexB: Int) {
    // only proceed if a and b have not been compiled away yet
    if(!dist.isActive(indexA) || !dist.isActive(indexB))
      return

    // check if the network is now inconsistent
    assert2(dist.getDistance(indexA, indexB) + dist.getDistance(indexB, indexA) >= 0,
      "Inconsistent network should have been caught earlier")

    // nothing to do
    if(indexA == indexB)
      return

    // if there is a structural timepoint rigidly fixed to another, record this relation and simplify
    // the distance matrix
    if(dist.getDistance(indexA,indexB) == -dist.getDistance(indexB,indexA)) {
      val originalDist = dist.getDistance(indexA, indexB)
      val tpA = timepointByIndex(indexA)
      val tpB = timepointByIndex(indexB)
      assert1(!rigidRelations.isAnchored(tpA))
      assert1(!rigidRelations.isAnchored(tpB))

      // record rigid relation
      rigidRelations.addRigidRelation(tpA, tpB, dist.getDistance(indexA, indexB))

      val (anchored, anchor) =
        if(rigidRelations.isAnchored(tpA)) (tpA, tpB)
        else if(rigidRelations.isAnchored(tpB)) (tpB,tpA)
        else throw new RuntimeException("No timepoint is considered as anchored after recording a new rigid relation")

      // compute updates that need to be made to the watch list
      // keys to be removed because, they are referring to the anchored timepoint
      val keysToRemove = watchedVarsByIndex.keys.filter(p => p._1 == toIndex(anchored) || p._2 == toIndex(anchored))
      // backup the watches stored under the keys to remove
      val watchesToReAdd = keysToRemove.toList.flatMap(k => watchedVarsByIndex(k))

      // remove the anchored timepoint from distance matrix
      dist.compileAwayRigid(toIndex(anchored), toIndex(anchor))
      timepointByIndex(toIndex(anchored)) = null
      nonRigidIndexes.remove(anchored)
      assert1(originalDist == rigidAwareDist(tpA, tpB))

      // remove outdated keys, and reinsert the corresponding keys
      watchedVarsByIndex --= keysToRemove
      for(w <- watchesToReAdd)
        addWatchedDistance(w._1, w._2)
    }
  }

  /** Transforms two distance-matrix IDs into the key used to refer to their watches */
  private def asWatchKey(id1: Int, id2: Int) : (Int, Int) = (math.min(id1, id2), math.max(id1, id2))

  /** Computes the key for the location of the watches on this edge. */
  private def watchKey(tp1: Timepoint, tp2: Timepoint) : (Int, Int) =
    asWatchKey(indexOfAnchor(tp1), indexOfAnchor(tp2))

  /** Returns true if updates of the given edge will be notified to the distance change listener. */
  def isWatched(tp1: Timepoint, tp2: Timepoint) : Boolean = {
    val k = watchKey(tp1, tp2)
    watchedVarsByIndex.getOrElse(k, Nil).contains((tp1, tp2))
  }

  def addWatchedDistance(tp1: Timepoint, tp2: Timepoint) {
    val key = watchKey(tp1, tp2)
    watchedVarsByIndex.getOrElseUpdate(key, mutable.ArrayBuffer()) += ((tp1, tp2))

    assert1(distanceChangeListener.nonEmpty, "Added a watch but no distance change listener plugged in.")
    assert2(isWatched(tp1, tp2))
    assert3(watchedVarsByIndex.toSeq.forall{case (k,v) => v.forall(p => k == watchKey(p._1, p._2))})
  }

  def removeWatchedDistance(tp1: Timepoint, tp2: Timepoint) {
    val k = watchKey(tp1, tp2)
    assert2(watchedVarsByIndex.contains(k), "Distance is not watched")
    assert2(watchedVarsByIndex(k).contains((tp1, tp2)))
    watchedVarsByIndex(k) -= ((tp1, tp2))
    if(watchedVarsByIndex(k).isEmpty)
      watchedVarsByIndex -= k
  }

  /** Record this time point as the global start of the STN */
  def recordTimePointAsStart(tp: Timepoint): Int = {
    if(!isKnown(tp))
      recordTimePoint(tp)
    setStart(tp)
    nonRigidIndexes(tp)
  }

  def setStart(start: Timepoint): Unit = {
    assert1(isKnown(start))
    assert1(optStart.isEmpty || optStart.get == start)
    optStart = Some(start)
    optEnd match {
      case Some(end) => enforceMinDelay(start, end, 0)
      case None =>
    }
  }

  /** Unifies this time point with the global end of the STN */
  def recordTimePointAsEnd(tp: Timepoint): Int = {
    if(!isKnown(tp))
      recordTimePoint(tp)
    setEnd(tp)
    nonRigidIndexes(tp)
  }

  def setEnd(end: Timepoint): Unit = {
    assert1(isKnown(end))
    assert1(optEnd.isEmpty || optEnd.get == end)
    optEnd = Some(end)
    for(tp <- timepoints.asScala) {
      enforceBefore(tp, end)
    }
    optStart match {
      case Some(start) => enforceMinDelay(start, end, 0)
      case None =>
    }
  }

  /** Returns true if the STN is consistent (might trigger a propagation */
  def isConsistent: Boolean = {
    assert4(coherentWrtBellmanFord)
    consistent &&
      (!shouldCheckPseudoControllability ||
        contingentLinks.forall(l => isDelayPossible(l.src, l.dst, l.min) && isConstraintPossible(l.src, l.dst, l.max)))
  }

  protected def addConstraint(u: Timepoint, v: Timepoint, w: Int): Unit =
    addMaxDelay(u, v, w)

  protected def isConstraintPossible(u: Timepoint, v: Timepoint, w: Int): Boolean =
    w + rigidAwareDist(v, u) >= 0


  /** Set the distance from the global start of the STN to tp to time */
  def setTime(tp: Timepoint, time: Int): Unit =
    optStart match {
      case Some(st) =>
        addMinDelay(st, tp, time)
        addMaxDelay(st, tp, time)
      case None => sys.error("This STN has no start timepoint")
    }


  /** Returns the minimal time from the start of the STN to u */
  def getEarliestTime(u: Timepoint): Int =
    optStart match {
      case Some(st) => minDelay(st, u)
      case None => sys.error("This STN has no start timepoint")
    }

  /** Returns the maximal time from the start of the STN to u */
  def getLatestTime(u: Timepoint): Int =
    optStart match {
      case Some(st) => maxDelay(st, u)
      case None => sys.error("This STN has no start timepoint")
    }

  /**
    * Computes the max delay from a given timepoint to all others using Bellman-Ford on the original edges.
    * This is expensive (O(V*E)) but is useful for providing a reference to compare to when debugging.
    */
  private def distancesFromWithBellmanFord(from: Timepoint) : Array[Int] = {
    // initialize distances
    val d = new Array[Int](99999)
    for(tp <- timepoints.asScala)
      d(tp.id) = INF
    d(from.id) = 0

    // compute distances
    val numIters = timepoints.size
    for(i <- 0 until numIters) {
      for(e <- originalEdges) {
        d(e.to.id) = Math.min(d(e.to.id), DistanceMatrix.plus(d(e.from.id), e.value))
      }
    }
    d
  }

  /**
    * Computes the max delay between two timepoints using Bellman-Ford on the original edges.
    * This is expensive (O(V*E)) but is useful for providing a reference to compare to when debugging.
    */
  private def distanceWithBellmanFord(from: Timepoint, to: Timepoint): Int = {
    distancesFromWithBellmanFord(from)(to.id)
  }

  /**
    * Determine whether the STN is consistent using Bellman-Ford on the original edges.
    * This is expensive (O(V*E)) but is useful for providing a reference to compare to when debugging.
    */
  private def consistentWithBellmanFord(): Boolean = {
    // when possible, use "end" as the source as it normally linked with all other timepoints
    val from = optEnd match {
      case Some(end) => end
      case None => timepoints.head
    }
  val d = distancesFromWithBellmanFord(from)

    // if a distance can still be updated, there is a negative cycle
    for(e <- originalEdges) {
      if(d(e.to.id) > d(e.from.id) + e.value)
        return false
    }
    true
  }

  /** Expensive method that computes a new APSP with one BellmanFord run for each vertex.
    * Returns false if at least one distance is different to the one in the incremental FloydWarshall */
  private def coherentWrtBellmanFord: Boolean = {
    for(tp <- timepoints.asScala) {
      val d = distancesFromWithBellmanFord(tp)
      for(to <- timepoints.asScala) {
        if(maxDelay(tp, to) != d(to.id))
          return false
      }
    }
    true
  }

  def enforceContingent(u: Timepoint, v: Timepoint, min: Int, max: Int): Unit = {
    addMinDelay(u, v, min)
    addMaxDelay(u, v, max)
    contingentLinks.append(new ContingentConstraint(u, v, min, max))
  }

  def getMaxDelay(u: Timepoint, v: Timepoint): Int = maxDelay(u, v)

  def checksPseudoControllability: Boolean = true

  def checksDynamicControllability: Boolean = false

  def controllability: Controllability = Controllability.PSEUDO_CONTROLLABILITY

  /** If there is a contingent constraint [min, max] between those two timepoints, it returns
    * Some((min, max).
    * Otherwise, None is returned.
    */
  def contingentDelay(from: Timepoint, to: Timepoint): Option[(Int, Int)] =
    contingentLinks.find(l => l.src == from && l.dst == to) match {
      case Some(x) => Some(x.min, x.max)
      case None => None
    }

  def getMinDelay(u: Timepoint, v: Timepoint): Int = minDelay(u, v)

  def start: Option[Timepoint] = optStart

  def end: Option[Timepoint] = optEnd

  def getConstraintsWithoutStructurals : IList[TemporalConstraint] = {
    /** Builds the neighborhood of a group of structural timepoints */
    def structuralNeighborhood(neighborhood: Set[Timepoint], nextNeighbors: Set[Timepoint]): Set[Timepoint] = {
      assert2(neighborhood.intersect(nextNeighbors).isEmpty)
      assert2(nextNeighbors.forall(_.isStructural))
      assert2(neighborhood.forall(_.isStructural))

      if(nextNeighbors.isEmpty)
        return neighborhood // no new nodes to process, return the current neighborhood
      val tp = nextNeighbors.head

      if(rigidRelations.isAnchored(tp) && !rigidRelations.anchorOf(tp).isStructural) {
        // node is rigid, do not consider its own neighbors
        return structuralNeighborhood(neighborhood + tp, nextNeighbors.tail)
      }
      // in other cases, the neighborhood is expanded with this node and the neighborhood of all its neighbors
      val directNeighbors = originalEdges // should include neighborhood of other (non-rigid?) structurals
        .filter(e => e.from == tp || e.to == tp)
        .flatMap(e => e.from :: e.to :: Nil)
        .filter(_.isStructural)
        .toSet
      return structuralNeighborhood(neighborhood+tp, nextNeighbors ++ (directNeighbors--neighborhood) -tp)
    }
    /** Returns the anchor of 'tp' if tp is anchored and 'tp' otherwise*/
    def anchorOrSelf(tp: Timepoint) =
      if(rigidRelations.isAnchor(tp)) tp else rigidRelations.anchorOf(tp)

    /** Returns all non-structural nodes that touch the structural neighborhood **/
    def connections(tp: Timepoint) = {
      assert1(tp.isStructural)
      assert1(rigidRelations.isAnchor(tp))
      val structuralNeighbors = structuralNeighborhood(Set(), Set(tp))
      val nonStructuralNeighbours = originalEdges
        .filter(e => structuralNeighbors.contains(e.from) || structuralNeighbors.contains(e.to))
        .flatMap(e => e.from :: e.to :: Nil)
        .filter(!_.isStructural)
        .toSet ++
        structuralNeighbors
          .filter(x => rigidRelations.isAnchored(x) && !rigidRelations._anchorOf(x).isStructural)
          .map(x => rigidRelations.anchorOf(x))
      nonStructuralNeighbours
    }

    val pairs = mutable.Set[(Timepoint,Timepoint)]()
    // consider all edges, with start/end timepoints projected on their anchors
    for(c <- originalEdges) {
      pairs += ((anchorOrSelf(c.from), anchorOrSelf(c.to)))
    }
    pairs.retain(p => !p._1.isStructural && !p._2.isStructural)
    for(tp <- timepoints.asScala if tp.isStructural && rigidRelations.isAnchor(tp)) {
      val neighborhood = connections(tp)
      for(tp1 <- neighborhood ; tp2 <- neighborhood) {
        pairs += ((tp1,tp2))
        pairs += ((tp2,tp1))
      }
    }
    pairs.retain(p => p._1 != p._2)

    // constraints between non structurals that are anchored
    for(tp <- timepoints.asScala if !tp.isStructural && rigidRelations.isAnchored(tp) && !rigidRelations.anchorOf(tp).isStructural) {
      pairs += ((tp, rigidRelations.anchorOf(tp)))
      pairs += ((rigidRelations.anchorOf(tp), tp))
    }

    return new IList(contingentLinks.toList ++
      pairs.map(p => new MinDelayConstraint(p._2, p._1, minDelay(p._2, p._1))))
  }

  def getOriginalConstraints : IList[TemporalConstraint] = {
    new IList(originalEdges.map(e => new MinDelayConstraint(e.to, e.from, -e.value)) ++ contingentLinks)
  }
}
