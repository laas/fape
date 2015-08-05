package planstack.anml.model.abs

import planstack.anml.model.abs.statements.{AbstractLogStatement, AbstractResourceStatement, AbstractStatement}
import planstack.anml.model.concrete.RefCounter
import planstack.anml.model.{AnmlProblem, LVarRef, PartialContext}
import planstack.anml.parser

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class AbstractDecomposition(parentContext:PartialContext) {
  val context = new PartialContext(Some(parentContext))

  val statements = ListBuffer[AbstractStatement]()
  def jStatements = seqAsJavaList(statements)

  def jActions = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractActionRef]).map(_.asInstanceOf[AbstractActionRef]))
  def jLogStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractLogStatement]).map(_.asInstanceOf[AbstractLogStatement]))
  def jResStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractResourceStatement]).map(_.asInstanceOf[AbstractResourceStatement]))
  def jTempConstraints = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractTemporalConstraint]).map(_.asInstanceOf[AbstractTemporalConstraint]))

}


object AbstractDecomposition {

  def apply(pb:AnmlProblem, context:PartialContext, pDec:parser.Decomposition, refCounter: RefCounter) : AbstractDecomposition = {
    val dec = new AbstractDecomposition(context)

    pDec.content.foreach(_ match {
      case constraint:parser.TemporalConstraint => dec.statements += AbstractTemporalConstraint(constraint)
      // constant function with no arguments is interpreted as local variable
      case const:parser.Constant => dec.context.addUndefinedVar(new LVarRef(const.name), const.tipe)
      case statement:parser.TemporalStatement => {
        dec.statements ++= StatementsFactory(statement, dec.context, pb, refCounter)
      }
    })

    dec
  }
}