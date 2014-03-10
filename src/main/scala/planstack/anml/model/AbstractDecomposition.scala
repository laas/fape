package planstack.anml.model

import scala.collection.mutable
import planstack.anml.ANMLException
import planstack.anml.parser
import scala.collection.mutable.ListBuffer

class AbstractActionRef(val name:String, val args:List[String], val localId:String)


object AbstractActionRef {

  private var nextID = 0

  /**
   * Produces a and abstract action ref and its associated TemporalStatements.
   * The temporal statements derive from parameters given as functions and not variables
   * @param pb
   * @param context
   * @param ar
   * @return
   */
  def apply(pb:AnmlProblem, context:PartialContext, ar:parser.ActionRef) : Pair[AbstractActionRef, List[AbstractTemporalStatement]] = {

    // for every argument, get a variable name and, optionaly, a temporal persistence if
    // the argument was given in the form of a function
    val args : List[Pair[String, Option[AbstractTemporalStatement]]] = ar.args map(argExpr => {
      argExpr match {
        case v:parser.VarExpr => {
          // argument is a variable
          if(!context.contains(v.variable)) {
            // var doesn't exists, add it to context
            context.addUndefinedVar(v.variable, "object")
          }
          (v.variable, None)
        }
        case f:parser.FuncExpr => {
          // this is a function f, create a new var v and add a persistence [all] f == v;
          val varName = context.getNewLocalVar("object")
          val ts = new AbstractTemporalStatement(
            TemporalAnnotation("start","end"),
            new AbstractPersistence(ParameterizedStateVariable(pb, context, f), varName))
          (varName, Some(ts))
        }
      }
    })

    val actionRefId =
      if(ar.id.nonEmpty)
        ar.id
      else {
        nextID += 1
        "actionRef"+(nextID-1)
      }

    val statements = args.map(_._2).flatten

    Pair(new AbstractActionRef(ar.name, args.map(_._1), actionRefId), statements)
  }

}


class AbstractDecomposition(parentContext:PartialContext) {
  val context = new PartialContext(Some(parentContext))

  val actions = ListBuffer[AbstractActionRef]()
  val precedenceConstraints = ListBuffer[Pair[Int,Int]]()
  val temporalStatements = ListBuffer[AbstractTemporalStatement]()

}


object AbstractDecomposition {

  /**
   * Extracts all leaves in a tree structures conposed
   * of Lists and Sets and where every leaf in an Int
   * @param in
   * @return
   */
  private def extractAllInt(in:Any) : Set[Int] = {
    in match {
      case i:Int => Set(i)
      case l:List[Any] => l.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case s:Set[Any] => s.foldLeft(Set[Int]())((set, in) => set ++ extractAllInt(in))
      case x => throw new ANMLException("Unsupported structure for extraction: " + x)
    }
  }

  private def addPrecedenceConstraints(dec:AbstractDecomposition, constraints:Any) {
    constraints match {
      case x:Int =>
      case s:Set[Any] => s.foreach(addPrecedenceConstraints(dec, _))
      case head :: Nil => addPrecedenceConstraints(dec, head)
      case head :: tail => {
        for(i <- extractAllInt(head) ; j <- extractAllInt(tail.head)) {
          dec.precedenceConstraints += ((i, j))
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