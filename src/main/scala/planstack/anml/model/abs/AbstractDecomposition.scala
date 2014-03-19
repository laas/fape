package planstack.anml.model.abs

import planstack.anml.ANMLException
import planstack.anml.parser
import scala.collection.mutable.ListBuffer
import planstack.anml.model.{AnmlProblem, PartialContext}
import collection.JavaConversions._

class AbstractDecomposition(parentContext:PartialContext) {
  val context = new PartialContext(Some(parentContext))

  val actions = ListBuffer[AbstractActionRef]()
  def jActions = seqAsJavaList(actions)

  val precedenceConstraints = ListBuffer[AbstractTemporalConstraint]()
  val temporalStatements = ListBuffer[AbstractTemporalStatement]()
}


object AbstractDecomposition {

  /**
   * Extracts all leaves in a tree structure composed
   * of Lists and Sets and where every leaf in an Int
   * @param in
   * @return
   */
  private def extractAllInt(in:Any) : Set[Int] = {
    in match {
      case i:Int => Set(i)
      case l:List[Any] => l.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case s:Set[Any] @unchecked => s.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case x => throw new ANMLException("Unsupported structure for extraction: " + x)
    }
  }

  private def addPrecedenceConstraints(dec:AbstractDecomposition, constraints:Any) {
    constraints match {
      case x:Int =>
      case s:Set[Any] @unchecked => s.foreach(addPrecedenceConstraints(dec, _))
      case head :: Nil => addPrecedenceConstraints(dec, head)
      case head :: tail => {
        for(i <- extractAllInt(head) ; j <- extractAllInt(tail.head)) {
          dec.precedenceConstraints += AbstractTemporalConstraint.before(dec.actions(i).localId, dec.actions(j).localId)
        }
        addPrecedenceConstraints(dec, head)
        addPrecedenceConstraints(dec, tail)
      }
    }
  }

  private def addPOActions(pb:AnmlProblem, dec:AbstractDecomposition, poActions:parser.PartiallyOrderedActionRef) : Any = {
    poActions match {
      case ar:parser.ActionRef => {
        val (actionRef, statements) = AbstractActionRef(pb, dec.context, ar)
        dec.actions += actionRef
        dec.temporalStatements ++= statements
        dec.actions.length -1
      }
      case unordered:parser.UnorderedActionRef => {
        unordered.list.map(addPOActions(pb, dec, _)).toSet
      }
      case ordered:parser.OrderedActionRef => ordered.list.map(addPOActions(pb, dec, _)).toList
    }
  }

  def apply(pb:AnmlProblem, context:PartialContext, pDec:parser.Decomposition) : AbstractDecomposition = {
    val dec = new AbstractDecomposition(context)

    val precedenceStruct = addPOActions(pb, dec, pDec.actions)
    addPrecedenceConstraints(dec, precedenceStruct)

    dec
  }
}