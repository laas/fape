package fr.laas.fape.constraints.stnu.morris

import java.io.{File, PrintWriter}

import DCMorris._
import fr.laas.fape.anml.model.concrete.{ContingentConstraint, MinDelayConstraint, TPRef, TemporalConstraint}
import fr.laas.fape.constraints.stnu.{Constraint, ElemStatus}

class TemporalNetwork(
                      /** Controllable timepoints */
                      val controllables: Set[Node],
                      /** Contingent timepoints that can always be observed */
                      val visibles: Set[Node],
                      /** Contingent timepoints that can never be observed */
                      val invisibles: Set[Node],
                      /** Contingent timepoints for which we don't know yet if they will be observed */
                       val possiblyObservables: Set[Node],
                      /** Constraints of the temporal network */
                       val constraints: List[Edge]
                     ) {

  lazy val contingents : Set[Node] = (visibles ++ invisibles) ++ possiblyObservables

  /** Move the given possibly observable nodes to the invisible set */
  def makeInvisible(tps: Set[Node]): TemporalNetwork = {
    assert(tps.forall(possiblyObservables.contains(_)))
    new TemporalNetwork(controllables, visibles, invisibles ++ tps, possiblyObservables -- tps, constraints)
  }

  /** Move the given possibly observable nodes to the visible set */
  def makeVisible(tps: Set[Node]): TemporalNetwork = {
    assert(tps.forall(possiblyObservables.contains(_)))
    new TemporalNetwork(controllables, visibles ++ tps, invisibles, possiblyObservables -- tps, constraints)
  }

  /** All possibly observable nodes are moved to the invisible set.
    * Constraint involving those are compiled away and the resulting additional constraints
    * are labeled to show what are the projection they enforce
    */
  def supposeNonObservable : TemporalNetwork = {
    val newEdges = PartialObservability.makePossiblyObservable(constraints, possiblyObservables)
    new TemporalNetwork(controllables, visibles, invisibles ++ possiblyObservables, Set(), newEdges)
  }

  /** Returns a new temporal network in which all invisible timepoints have been compiled away */
  def withoutInvisible :TemporalNetwork = {
    val newEdges = PartialObservability.makeNonObservable(constraints, invisibles.toList)
    new TemporalNetwork(controllables, visibles, Set(), possiblyObservables, newEdges)
  }

  /** transform A == [d1,d2] => B into A -- [d1,d1] -> A' == [0,d2-d1] => B */
  def normalForm : TemporalNetwork = {
    val firstAdditionalNode = constraints.flatMap(e => List(e.from,e.to)).max +1
    var nextNode = firstAdditionalNode
    def newNode() :Node = {nextNode+=1 ; nextNode-1}
    val addNodes = constraints.collect { case l:Lower => (l.to, (newNode(), l.d)) }.toMap
    val newEdges = constraints.flatMap {
      case Lower(from, to, d, lbl, projs) =>
        val (virt, dist) = addNodes(to)
        assert(dist == d, constraints.mkString("\n"))
        Lower(virt, to, 0, lbl, projs) ::
          Req(from, virt, d, Set()) :: Req(virt, from, -d, Set()) ::Nil
      case Upper(from, to, d, lbl, projs) =>
        Upper(from, addNodes(from)._1, d+addNodes(from)._2, lbl, projs) :: Nil
      case r:Req =>
        r :: Nil
    }
    new TemporalNetwork(
      controllables ++ (firstAdditionalNode until nextNode),
      visibles, invisibles, possiblyObservables, newEdges)
  }

  def export(filename: String): Unit = {
    val sb = new StringBuilder
    sb.append("(define\n")
    sb.append("  (:timepoints\n")
    for(tp <- controllables)
      sb.append(s"    (controllable $tp)\n")
    for(tp <- visibles)
      sb ++= s"    (visible $tp)\n"
    for(tp <- invisibles)
      sb ++= s"    (invisible $tp)\n"
    for(tp <- possiblyObservables)
      sb ++= s"    (observable $tp)\n"
    sb.append("  )\n")
    sb.append("  (:constraints\n")
    for(c <- constraints)
      sb ++= "    " + (c match {
        case Req(from, to, d, _) => s"(req $from $to $d)"
        case Upper(from, to, d, lbl, _) => s"(upper $from $to $d $lbl)"
        case Lower(from, to, d, lbl, _) => s"(lower $from $to $d $lbl)"
      }) + "\n"
    sb.append("  )\n")
    sb.append(")\n")

    val pw = new PrintWriter(new File(filename))
    pw.write(sb.toString())
    pw.close()
  }

  lazy val longestContingentChain = {
    val edges = constraints.collect { case x:Upper => x }
    val next = edges.map(e => (e.from, e.to)).toMap

    var longestChain = 0
    for(n <- next.keySet) {
      var cur = n
      var chainSize = 0
      while(next.contains(cur)) {
        cur = next(cur)
        chainSize += 1
      }
      longestChain = Math.max(longestChain, chainSize)
    }
    longestChain
  }
}



object TemporalNetwork {

  def build[ID](constraints: Seq[TemporalConstraint], observed: Set[TPRef], observable: Set[TPRef]) : TemporalNetwork = {
    implicit def toInt(tp: TPRef): Node = tp.id
    val tps = constraints.flatMap(c => List(c.src, c.dst)).toSet
    val tpsFromInt = tps.map(tp => (tp.id, tp)).toMap

    val edges = constraints.flatMap(c => c match {
      case c:ContingentConstraint =>
        Lower(c.src, c.dst, c.min.get, c.dst, Set()) :: Upper(c.dst, c.src, -c.max.get, c.dst, Set()) :: Nil
      case c: MinDelayConstraint =>
        Req(c.dst, c.src, -c.minDelay.get, Set()) :: Nil
    }).toList

    val nonObservable = tps.filter(tp => tp.genre.isContingent && !observable.contains(tp) && !observed.contains(tp))

    val controllables = tps.filter(tp => tp.genre.isDispatchable || tp.genre.isStructural)

    new TemporalNetwork(controllables.map(_.id), observed.map(_.id), nonObservable.map(_.id), observable.map(_.id), edges)
  }

  def loadFromFile(filename: String) : TemporalNetwork = {
    val fileContent = scala.io.Source.fromFile(filename).getLines().foldLeft("")((all: String, curr: String) => all + curr + "\n")
    val ret = Parser.parseAll(Parser.problem, fileContent)
    if(ret.isEmpty) {
      // show error
      println(ret)
      sys.error("Was not able to parse "+filename)
    } else {
      val (tps, edges) = ret.get
      val controllables = tps.collect { case ("controllable", x) => x }.toSet
      val visibles = tps.collect { case ("visible", x) => x }.toSet
      val observable = tps.collect { case ("observable",x) => x }.toSet
      val hidden = tps.collect { case ("hidden", x) => x }.toSet
      new TemporalNetwork(controllables, visibles, hidden, observable, edges)
    }
  }
}