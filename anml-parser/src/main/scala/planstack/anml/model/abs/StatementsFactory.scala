package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements._
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.model.concrete.RefCounter
import planstack.anml.parser.{FuncExpr, NumExpr, VarExpr}
import planstack.anml.pending.{IntExpression, IntLiteral}
import planstack.anml.{ANMLException, parser}

object StatementsFactory {

  def normalizeExpr(expr : parser.Expr, context:AbstractContext, pb:AnmlProblem) : FuncExpr = {
    expr match {
      case parser.FuncExpr(nameParts, argList) => {
        if(nameParts.size == 2 && !pb.instances.containsType(nameParts.head)) {
          // function prefixed with a var/constant.
          // find its type (part of the function name) and add the var as the first argument
          val headType = context.getType(nameParts.head)
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
    *
    * @param annotatedStatement Statement (with temporal annotations) to translate
   * @param context Context in which the annotated statement appears
   * @param pb Problem in which the statement appears
   * @return An equivalent list of AbstractStatement
   */
  def apply(annotatedStatement : parser.TemporalStatement, context:AbstractContext, pb:AnmlProblem, refCounter: RefCounter) : (Option[AbstractStatement],List[AbstractConstraint]) = {
    val (optStatement, constraints) = StatementsFactory(annotatedStatement.statement, context, pb, refCounter)

    annotatedStatement.annotation match {
      case None =>
        optStatement match {
          case Some(ls :AbstractLogStatement) if !ls.sv.func.isConstant =>
            println("Warning: log statement with no temporal annotation: "+ls)
          case _ =>
        }
        (optStatement, constraints)
      case Some(parsedAnnot) => {
        val annot = AbstractTemporalAnnotation(parsedAnnot)
        assert(optStatement.nonEmpty, "Temporal annotation on something that is not a statement or a task: "+constraints)
        (optStatement, optStatement.get.getTemporalConstraints(annot) ::: constraints)
      }
    }
  }

  def apply(statement : parser.Statement, context:AbstractContext, pb : AnmlProblem, refCounter: RefCounter) : (Option[AbstractStatement], List[AbstractConstraint]) = {
    statement match {
      case s:parser.SingleTermStatement => {
        if (isStateVariable(s.term, context, pb)) {
          // state variable alone. Make sure it is a boolean make it a persistence condition at true
          val sv = asStateVariable(s.term, context, pb)
          assert(sv.func.valueType == "boolean", "Non-boolean function as a single term statement: "+s)
          if(sv.func.isConstant)
            (None, List(new AbstractEqualityConstraint(sv, context.getLocalVar("true"), LStatementRef(s.id))))
          else
            (Some(new AbstractPersistence(sv, context.getLocalVar("true"), LStatementRef(s.id))), Nil)
        } else {
          // it should be an action, but we can't check since this action might not have been parsed yet
          //assert(pb.containsAction(s.term.functionName), s.term.functionName + " is neither a function nor an action")
          val e = normalizeExpr(s.term, context, pb)
          val task = new AbstractTask("t-"+s.term.functionName, e.args.map(v => context.getLocalVar(v.variable)), LActRef(s.id))
          (Some(task), List(AbstractMinDelay(task.start, task.end, IntExpression.lit(1))))
        }
      }
      case parser.TwoTermsStatement(e1, op, e2, id) => {
        if (isStateVariable(e1, context, pb)) {
          val sv = asStateVariable(e1, context, pb)
          if(sv.isResource && !sv.func.isConstant) {
            assert(e2.isInstanceOf[NumExpr], "Non numerical expression at the right side of a resource statement.")
            val rightValue = e2.asInstanceOf[NumExpr].value
            op.op match {
              case ":use" =>
                (Some(new AbstractUseResource(sv, rightValue, LStatementRef(id))), Nil)
              case ":produce" =>
                (Some(new AbstractProduceResource(sv, rightValue, LStatementRef(id))), Nil)
              case ":lend" =>
                (Some(new AbstractLendResource(sv, rightValue, LStatementRef(id))), Nil)
              case ":consume" =>
                (Some(new AbstractConsumeResource(sv, rightValue, LStatementRef(id))), Nil)
              case ":=" =>
                (Some(new AbstractSetResource(sv, rightValue, LStatementRef(id))), Nil)
              case op if Set("<","<=",">",">=").contains(op) =>
                (Some(new AbstractRequireResource(sv, op, rightValue, LStatementRef(id))), Nil)
              case x =>
                throw new ANMLException("Operator "+x+" is not valid in the resource statement: "+statement)
            }
          } else {
            if(sv.func.isConstant) {
              // those are binding constraints (on constant functions)
              e2 match {
                case e2: NumExpr => {
                  assert(sv.func.valueType == "integer", "Function "+sv.func+" does not have the type integer.")
                  assert(op.op == ":=")
                  val value = e2.asInstanceOf[NumExpr].value.toInt
                  (None, List(new AbstractIntAssignmentConstraint(sv, value, LStatementRef(id))))
                }
                case e2: VarExpr => {
                  // f(a, b) == c  and f(a, b) != c
                  val variable = context.getLocalVar(e2.functionName)
                  op.op match {
                    case "==" => (None, List(new AbstractEqualityConstraint(sv, variable, LStatementRef(id))))
                    case "!=" => (None, List(new AbstractInequalityConstraint(sv, variable, LStatementRef(id))))
                    case ":=" => (None, List(new AbstractAssignmentConstraint(sv, variable, LStatementRef(id))))
                    case x => throw new ANMLException("Wrong operator in statement: " + statement)
                  }
                }
                case e2: FuncExpr => {
                  //  f(a, b) == g(d, e)  and  f(a, b) != g(d, e)
                  assert(isStateVariable(e2, context, pb), s"$e2 is not a state variable. From statement: $statement")
                  val rightSv = asStateVariable(e2, context, pb)
                  assert(rightSv.func.isConstant)

                  if (op.op == "==") {
                    // f(a, b) == g(d, e)
                    val sharedType =
                      if (sv.func.valueType == rightSv.func.valueType)
                        sv.func.valueType
                      else if (pb.instances.subTypes(sv.func.valueType).contains(rightSv.func.valueType))
                        rightSv.func.valueType
                      else if (pb.instances.subTypes(rightSv.func.valueType).contains(sv.func.valueType))
                        sv.func.valueType
                      else
                        throw new ANMLException("The two state variables have incompatible types: " + sv + " -- " + rightSv)

                    val variable = context.getNewUndefinedVar(sharedType, refCounter)
                    (None, List(
                      new AbstractEqualityConstraint(sv, variable, LStatementRef(id)),
                      new AbstractEqualityConstraint(rightSv, variable, new LStatementRef())
                    ))
                  } else {
                    assert(op.op == "!=", "Unsupported operator in statement: " + statement)
                    val v1 = context.getNewUndefinedVar(sv.func.valueType, refCounter)
                    val v2 = context.getNewUndefinedVar(rightSv.func.valueType, refCounter)
                    (None, List(
                      new AbstractEqualityConstraint(sv, v1, LStatementRef(id)),
                      new AbstractEqualityConstraint(rightSv, v2, new LStatementRef()),
                      new AbstractVarInequalityConstraint(v1, v2, new LStatementRef())
                    ))
                  }
                }
              }
            } else {
              assert(e2.isInstanceOf[VarExpr], "Compound expression at the right side of a statement: " + statement)
              val variable = context.getLocalVar(e2.functionName)
              assert(pb.instances.isValueAcceptableForType(variable, sv.func.valueType, context),
                "In the statement: " + statement + ", " + context.getType(variable) + "is not a subtype of " + sv.func.valueType)

              //logical statement
              op.op match {
                case "==" => (Some(new AbstractPersistence(sv, variable, LStatementRef(id))), Nil)
                case ":=" => (Some(new AbstractAssignment(sv, variable, LStatementRef(id))), Nil)
                case _ => throw new ANMLException("Invalid operator in: " + statement)
              }
            }
          }
        } else {
          // e1 is not a state variable, it can only be something like:
          // a == b or a != b
          assert(e1.isInstanceOf[VarExpr], "Left part is not a variable and was not identified as a function: "+statement)
          assert(e2.isInstanceOf[VarExpr], "Right part is not a variable and was not identified as a function: "+statement)
          val v1 = context.getLocalVar(e1.functionName)
          val v2 = context.getLocalVar(e2.functionName)
          op.op match {
            case "==" => (None, List(new AbstractVarEqualityConstraint(v1, v2, LStatementRef(id))))
            case "!=" => (None, List(new AbstractVarInequalityConstraint(v1, v2, LStatementRef(id))))
            case _ => throw new ANMLException("Unsupported statement: "+statement)
          }
        }
      }
      case parser.ThreeTermsStatement(e1, o1, e2, o2, e3, id) => {
        assert(o1.op == "==" && o2.op == ":->", "This statement is not a valid transition: "+statement)
        assert(e2.isInstanceOf[VarExpr], "Compound expression in the middle of a transition: " + statement)
        assert(e3.isInstanceOf[VarExpr], "Compound expression in the right side of a transition: " + statement)
        assert(isStateVariable(e1, context, pb), "Left term does not seem to be a state variable: "+e1)
        val sv = asStateVariable(e1, context, pb)
        val v1 = context.getLocalVar(e2.functionName)
        val v2 = context.getLocalVar(e3.functionName)
        val tipe = sv.func.valueType
        assert(pb.instances.isValueAcceptableForType(v1, tipe, context),
          "Type: "+context.getType(v1)+" is not a subtype of "+tipe+
            " which is the type of the state variable. In statement: "+statement)
        assert(pb.instances.isValueAcceptableForType(v2, tipe, context),
          "Type: "+context.getType(v2)+" is not a subtype of "+tipe+
            " which is the type of the state variable. In statement: "+statement)
        (Some(new AbstractTransition(sv, v1, v2, LStatementRef(id))), Nil)
      }
    }
  }

}
