package planstack.anml.model

import planstack.anml.{ANMLException, parser}
import scala.collection.mutable
import planstack.anml.parser._
import planstack.anml.parser.TemporalStatement
import planstack.anml.parser.ActionRef

trait TemporalContext {

}

class Decomposition {
  val actions = mutable.ArrayBuffer[ActionRef]()

  var precedenceConstraints = List[Pair[Int,Int]]()

}

object Decomposition {

  def extractAllInt(in:Any) : Set[Int] = {
    in match {
      case i:Int => Set(i)
      case l:List[Any] => l.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case s:Set[Any] => s.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case x => throw new ANMLException("Unsupported structure for extraction: " + x)
    }
  }

  def addPrecedenceConstraints(dec:Decomposition, constraints:Any) {
    constraints match {
      case x:Int =>
      case s:Set[Any] => s.foreach(addPrecedenceConstraints(dec, _))
      case head :: Nil => addPrecedenceConstraints(dec, head)
      case head :: tail => {
        for(i <- extractAllInt(head) ; j <- extractAllInt(tail.head)) {
          dec.precedenceConstraints = (i, j) :: dec.precedenceConstraints
        }
        addPrecedenceConstraints(dec, head)
        addPrecedenceConstraints(dec, tail)
      }
    }
  }

  def addPOActions(dec:Decomposition, poActions:PartiallyOrderedActionRef) : Any = {
    poActions match {
      case ar:ActionRef => {
        dec.actions += ar
        dec.actions.length -1
      }
      case unordered:UnorderedActionRef => {
        unordered.list.map(addPOActions(dec, _)).toSet
      }
      case ordered:OrderedActionRef => ordered.list.map(addPOActions(dec, _)).toList
    }
  }

  def apply(pDec:parser.Decomposition, action:AbstractAction) : Decomposition = {
    val dec = new Decomposition

    val precedenceStruct = addPOActions(dec, pDec.actions)
    addPrecedenceConstraints(dec, precedenceStruct)

    dec
  }
}

class AbstractAction(val name:String, val args:List[String], val context:PartialContext)  {

  val decompositions = mutable.ArrayBuffer[Decomposition]()
  val temporalStatements = mutable.ArrayBuffer[AbstractTemporalStatement]()

}

object AbstractAction {

  def apply(act:parser.Action, pb:AnmlProblem) : AbstractAction = {
    val action = new AbstractAction(act.name, act.args.map(_.name), new PartialContext(Some(pb.context)))

    act.args foreach(arg => {
      action.context.addUndefinedVar(arg.name, arg.tipe)
    })

    act.content foreach( _ match {
      case ts:parser.TemporalStatement => {
        action.temporalStatements += AbstractTemporalStatement(pb, ts)
      }
      case dec:parser.Decomposition => {
        action.decompositions += Decomposition(dec, action)
      }
    })

    action
  }
}