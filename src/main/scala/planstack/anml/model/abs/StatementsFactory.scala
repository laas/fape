package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements._
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.parser.{NumExpr, FuncExpr, VarExpr}
import planstack.anml.{ANMLException, parser}

import scala.collection.mutable.ArrayBuffer

object StatementsFactory {

  def normalizeExpr(expr : parser.Expr, context:AbstractContext, pb:AnmlProblem) : FuncExpr = {
    expr match {
      case parser.FuncExpr(nameParts, argList) => {
        if(nameParts.size == 2 && !pb.instances.containsType(nameParts.head)) {
          // function prefixed with a var/constant.
          // find its type (part of the function name) and add the var as the first argument
          val headType = context.getType(new LVarRef(nameParts.head))
          parser.FuncExpr(pb.instances.getQualifiedFunction(headType,nameParts.tail.head), parser.VarExpr(nameParts.head)::argList)
        } else if(nameParts .size <= 2) {
          parser.FuncExpr(nameParts, argList)
        } else {
          throw new ANMLException("This func expr is not valid: "+expr)
        }
      }
      case parser.VarExpr(x) => parser.FuncExpr(List(x), Nil)
      case _ => throw new ANMLException("Unsupported expression: "+expr)
    }
  }

  def isStateVariable(expr : parser.Expr, context:AbstractContext, pb:AnmlProblem) : Boolean = {
    val e = normalizeExpr(expr, context, pb)
    pb.functions.isDefined(e.functionName)
  }

  def asStateVariable(expr:parser.Expr, context:AbstractContext, pb:AnmlProblem) = {
    AbstractParameterizedStateVariable(pb, context, expr)
  }

  /**
   * Transforms an annotated statement into its corresponding statement and the temporal constraints that applies
   * to its time-points (derived from the annotation).
   * @param annotatedStatement Statement (with temporal annotations) to translate
   * @param context Context in which the annotated statement appears
   * @param pb Problem in which the statement appears
   * @return An equivalent list of AbstractStatement
   */
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, pb:AnmlProblem) : List[AbstractStatement] = {
    val s = StatementsFactory(annotatedStatement.statement, context, pb)

    annotatedStatement.annotation match {
      case None => List(s)
      case Some(parsedAnnot) => {
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        s :: s.getTemporalConstraints(annot)
      }
    }
  }

  def apply(statement : parser.Statement, context:AbstractContext, pb : AnmlProblem) : AbstractStatement = {
    statement match {
      case s:parser.SingleTermStatement => {
        if (isStateVariable(s.term, context, pb)) {
          // state variable alone. Make sure it is a boolean make it a persistence condition at true
          val sv = asStateVariable(s.term, context, pb)
          assert(sv.func.valueType == "boolean")
          new AbstractPersistence(sv, LVarRef("true"), LStatementRef(s.id))
        } else {
          // it should be an action, but we can't check since this action might not have been parsed yet
          //assert(pb.containsAction(s.term.functionName), s.term.functionName + " is neither a function nor an action")
          val e = normalizeExpr(s.term, context, pb)
          new AbstractActionRef(s.term.functionName, e.args.map(v => LVarRef(v.variable)), LActRef(s.id))
        }
      }
      case parser.TwoTermsStatement(e1, op, e2, id) => {
        if (isStateVariable(e1, context, pb)) {
          val sv = asStateVariable(e1, context, pb)
          if(sv.isResource) {
            assert(e2.isInstanceOf[NumExpr], "Non numerical expression at the right side of a resource statement.")
            val rightValue = e2.asInstanceOf[NumExpr].value
            op.op match {
              case ":use" => new AbstractUseResource(sv, rightValue, LStatementRef(id))
              case ":produce" => new AbstractProduceResource(sv, rightValue, LStatementRef(id))
              case ":lend" => new AbstractLendResource(sv, rightValue, LStatementRef(id))
              case ":consume" => new AbstractConsumeResource(sv, rightValue, LStatementRef(id))
              case ":=" => new AbstractSetResource(sv, rightValue, LStatementRef(id))
              case op if(Set("<","<=",">",">=").contains(op)) => new AbstractRequireResource(sv, op, rightValue, LStatementRef(id))
              case x => throw new ANMLException("Operator "+x+" is not valid in the resource statement: "+statement)
            }
          } else {
            assert(e2.isInstanceOf[VarExpr], "Compound expression at the right side of a statement: " + statement)
            val variable = LVarRef(e2.functionName)
            assert(pb.instances.subTypes(sv.func.valueType).contains(context.getType(variable)),
              "In the statement: " + statement + ", " + context.getType(variable) + "is not a subtype of " + sv.func.valueType)

            if (sv.func.isInstanceOf[NumFunction]) {
              // resource statement
              throw new ANMLException("Resources statements are not supported yet.")
            } else {
              //logical statement
              op.op match {
                case "==" => new AbstractPersistence(sv, variable, LStatementRef(id))
                case ":=" => new AbstractAssignment(sv, variable, LStatementRef(id))
                case _ => throw new ANMLException("Invalid operator in: " + statement)
              }
            }
          }
        } else {
          throw new ANMLException("Error: unhandled statement: "+statement)
        }
      }
      case parser.ThreeTermsStatement(e1, o1, e2, o2, e3, id) => {
        assert(o1.op == "==" && o2.op == ":->", "This statement is not a valid transition: "+statement)
        assert(e2.isInstanceOf[VarExpr], "Compound expression in the middle of a transition: " + statement)
        assert(e3.isInstanceOf[VarExpr], "Compound expression in the right side of a transition: " + statement)
        assert(isStateVariable(e1, context, pb))
        val sv = asStateVariable(e1, context, pb)
        val v1 = LVarRef(e2.functionName)
        val v2 = LVarRef(e3.functionName)
        val tipe = sv.func.valueType
        assert(pb.instances.subTypes(tipe).contains(context.getType(v1)))
        assert(pb.instances.subTypes(tipe).contains(context.getType(v2)))
        new AbstractTransition(sv, v1, v2, LStatementRef(id))
      }
    }
  }

}
