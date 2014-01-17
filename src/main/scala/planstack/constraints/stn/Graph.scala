package planstack.constraints.stn

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


trait Graph {
  var numVertices = 0
  def addVertex() : Int

  def setEdge(v1:Int, v2:Int, w:Int) : Boolean

  def getWeight(v1:Int, v2:Int) : Weight

  def outEdges(v:Int) : Seq[Edge]
  def inEdges(v:Int) : Seq[Edge]
  def getEdges : Seq[Edge]

  override def clone() : Graph = { throw new Exception("This is an abstract method Graph.clone") }
}

class Edge(val orig:Int, val dest:Int, val w:Int) {
  override def toString : String = "(%d, %d, %d)".format(orig, dest, w)
}

class AdjacencyList(val outedges:ArrayBuffer[List[Edge]],val inedges:ArrayBuffer[List[Edge]]) extends Graph {

  numVertices = outedges.length

  def this() = this(new ArrayBuffer[List[Edge]](0), new ArrayBuffer[List[Edge]](0))

  //val inedges = new ArrayBuffer[List[Edge]](0)
  //val outedges = new ArrayBuffer[List[Edge]](0)

  def addVertex() : Int = {
    inedges.append(List[Edge]())
    outedges.append(List[Edge]())
    numVertices += 1
    return numVertices - 1
  }

  def setEdge(v1:Int, v2:Int, w:Int) : Boolean = {
    val newEdge = new Edge(v1, v2, w)

    outedges(v1) = newEdge :: outedges(v1).filter(e => e.dest != v2)
    inedges(v2) = newEdge :: inedges(v2).filter(e => e.orig != v1)

    true
  }

  def getWeight(v1:Int, v2:Int) : Weight = {
    val opt = outedges(v1).find(e => e.dest == v2)
    opt match {
      case Some(e) => new Weight(e.w)
      case _ => Weight.InfWeight
    }
  }

  def outEdges(v:Int) : Seq[Edge] = outedges(v)
  def inEdges(v:Int) : Seq[Edge] = inedges(v)
  def getEdges : Seq[Edge] = {
    var alledges = List[Edge]()
    outedges.foreach(edgelist => alledges = alledges ++ edgelist)
    alledges
  }

  override def clone() : AdjacencyList = {
    new AdjacencyList(outedges.clone(), inedges.clone())
  }
}

class AdjacencyListWithBool(sup_outedges : ArrayBuffer[List[Edge]],
                            sup_inedges : ArrayBuffer[List[Edge]],
                            var edges : mutable.BitSet,
                            var bitsetSize:Int) extends AdjacencyList(sup_outedges, sup_inedges) {

  def this() = this(new ArrayBuffer[List[Edge]](0), new ArrayBuffer[List[Edge]](0),
                    new mutable.BitSet(10 * 10), 10)

  def idtovars(i:Int) : Tuple2[Int, Int] = {
    (i / bitsetSize, i % bitsetSize)
  }

  def varstoid(t:Tuple2[Int, Int]) : Int = varstoid(t._1, t._2)

  def varstoid(i:Int, j:Int) : Int = i * bitsetSize + j

  override def addVertex() = {
    if(numVertices >= bitsetSize) {
      val save = edges.toArray.map( idtovars(_) )
      bitsetSize = numVertices + 10
      edges = new mutable.BitSet(bitsetSize * bitsetSize)
      save.foreach( vars => edges.add(varstoid(vars)) )
    }
    super.addVertex()
  }

  override def setEdge(v1:Int, v2:Int, w:Int) : Boolean = {
    val newEdge = new Edge(v1, v2, w)
    if(edges.contains( varstoid((v1,v2)))) {
      outedges(v1) = newEdge :: outedges(v1).filter(e => e.dest != v2)
      inedges(v2) = newEdge :: inedges(v2).filter(e => e.orig != v1)
    } else {
      outedges(v1) = newEdge :: outedges(v1)
      inedges(v2) = newEdge :: inedges(v2)
      edges.add(varstoid(v1,v2))
    }

    true
  }

  override def getWeight(v1:Int, v2:Int) : Weight = {

    if(edges.contains( varstoid((v1,v2)))) {
      val opt = outedges(v1).find(e => e.dest == v2)
      opt match {
        case Some(e) => new Weight(e.w)
        case _ => throw new Exception("planstack.constraints.stn.Problem with bitset, no edge while it should be here")
      }
    } else {
      Weight.InfWeight
    }
  }

  override def clone() : AdjacencyListWithBool = {
    new AdjacencyListWithBool(outedges.clone(), inedges.clone(), edges.clone(), bitsetSize)
  }
}

class SimpleGraph extends Graph {

  val edges = new ArrayBuffer[mutable.HashMap[Int, Weight]]

  def addVertex() : Int = {
    edges += new mutable.HashMap[Int, Weight]()
    numVertices += 1
    return numVertices -1
  }

  def setEdge(v1:Int, v2:Int, w:Int) : Boolean = {
    val added = !edges(v1).contains(v2)
    edges(v1).put(v2, new Weight(w))
    println("add %d %d %d ".format(v1, v2, w))
    return added
  }

  def getWeight(v1:Int, v2:Int) : Weight =
    edges(v1).getOrElse(v2, Weight.InfWeight)

  def outEdges(v: Int): IndexedSeq[Edge] = {
    for(i <- 0 to numVertices-1 ;
        if edges(v).contains(i) ;
        if !edges(v)(i).inf) yield {
      //println("%d %d ".format(v, i))
      new Edge(v, i, edges(v)(i).w)
    }
  }

  def inEdges(v: Int): IndexedSeq[Edge] = {
    for(i <- 0 to numVertices-1 ;
        if edges(i).contains(v) ;
        if !edges(i)(v).inf) yield {
      //println("%d %d ".format(v, i))
      new Edge(i, v, edges(i)(v).w)
    }
  }

  def getEdges : IndexedSeq[Edge] = {
    for(i <- 0 to numVertices-1 ; j <- 0 to numVertices-1 ;
        if edges(i).contains(j) ;
        if !edges(i)(j).inf) yield {
      new Edge(i, j, edges(i)(j).w)
    }
  }
}