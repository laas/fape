package fr.laas.fape.anml.parser

import fr.laas.fape.anml.ANMLException

import scala.util.Success
import scala.util.parsing.combinator._


sealed trait AnmlBlock
sealed trait ActionContent
sealed trait DecompositionContent
sealed trait TypeContent



case class ForAll(args: List[Argument], content:List[DecompositionContent]) extends AnmlBlock with ActionContent with DecompositionContent

case class TemporalStatement(annotation:Option[TemporalAnnotation], statement:Statement) extends AnmlBlock with ActionContent with DecompositionContent

case class TemporalAnnotation(start:RelativeTimepoint, end:RelativeTimepoint, flag:String) {
  require(flag == "is" || flag == "contains")
}

case class RelativeTimepoint(tp:Option[TimepointRef], delta:Int) {
  def this(str:String) = this(Some(new Timepoint(str)), 0)
  def this(abs:Int) = this(None, abs)
}

trait TimepointRef
case class ExtractedTimepoint(extractor:String, intervalId:String) extends TimepointRef {
  require(extractor == "start" || extractor == "end")
}
case class Timepoint(id: String) extends TimepointRef

case class Operator(op:String)

sealed abstract class Statement(id:String)

case class SingleTermStatement(val term : Expr, val id:String) extends Statement(id)
case class TwoTermsStatement(left:Expr, op:Operator, right:Expr, id:String) extends Statement(id)
case class ThreeTermsStatement(left:Expr, op1:Operator, middle:Expr, op2:Operator, right:Expr, id:String) extends Statement(id)
case class OrderedStatements(statements: List[Statement], id: String) extends Statement(id)
case class UnorderedStatements(statements: List[Statement], id: String) extends Statement(id)



case class Action(name:String, args:List[Argument], content:List[ActionContent]) extends AnmlBlock

object Motivated extends ActionContent

sealed trait Duration extends ActionContent {
  if(minDur.isInstanceOf[NumExpr]) {
    val f = minDur.asInstanceOf[NumExpr].value
    assert((f - f.toInt.toFloat) == 0.0, "Duration is not an integer: "+minDur)
  }
  if(maxDur.isInstanceOf[NumExpr]) {
    val f = maxDur.asInstanceOf[NumExpr].value
    assert((f - f.toInt.toFloat) == 0.0, "Duration is not an integer: "+maxDur)
  }

  def minDur : Expr
  def maxDur : Expr
}

case class ExactDuration(dur : Expr) extends Duration {
  override def minDur = dur
  override def maxDur = dur
}

case class UncertainDuration(minDur : Expr, maxDur : Expr) extends Duration

sealed trait PType
case class PSimpleType(name: String) extends PType
case class PDisjunctiveType(types: Set[PSimpleType]) extends PType

case class Argument(tipe:PType, name:String)

case class Decomposition(content:Seq[DecompositionContent]) extends ActionContent

sealed trait Expr {
  def functionName : String
  def asANML : String
}

case class Word(w:String) extends Expr {
  def functionName = w
  def asANML = w.toString
}
case class ChainedExpr(left:Expr, right:Expr) extends Expr {
  require(!right.isInstanceOf[ChainedExpr])
  def functionName = s"$left.$right"
  def asANML = s"${left.asANML}.${right.asANML}"
}
case class VarExpr(variable:String) extends Expr {
  override def functionName = variable
  def asANML = variable.toString
}
case class FuncExpr(funcExpr:Expr, args:List[Expr]) extends Expr {
  override def functionName = funcExpr.functionName
  def asANML = s"${funcExpr.asANML}(${args.map(_.asANML).mkString(",")})"
}

case class SetExpr(parts: Set[Expr]) extends Expr {
  override def functionName: String = ???

  override def asANML: String = "{"+parts.map(_.asANML).mkString(", ")+"}"
}

case class NumExpr(value : Float) extends Expr {
  override def functionName = value.toString
  def asANML = value.toString
}

trait TemporalConstraint extends DecompositionContent with ActionContent with AnmlBlock

case class ReqTemporalConstraint(tp1:TimepointRef, operator:String, tp2:TimepointRef, delta:Int)
  extends TemporalConstraint  {
  require(operator == "=" || operator == "<")
}
case class ContingentConstraint(src:TimepointRef, dst:TimepointRef, min:Int, max:Int) extends TemporalConstraint

class Function(val name:String, val args:List[Argument], val tipe:PType, val isConstant:Boolean) extends AnmlBlock with TypeContent

case class SymFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:PType,
    override val isConstant:Boolean)
  extends Function(name, args, tipe, isConstant)
{
  assert(tipe.toString != "integer" && tipe.toString != "float", "Symbolic function with a numeric type: "+this)
}

class NumFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:PType,
    override val isConstant:Boolean,
    val resourceType:Option[String])
  extends Function(name, args, tipe, isConstant)
{
  resourceType match {
    case None => // OK
    case Some(x) => assert(x=="consumable" | x=="producible" | x=="reusable" | x=="replenishable")
  }
}

case class IntFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:PType,
    override val isConstant:Boolean,
    minValue:Int,
    maxValue:Int,
    override val resourceType:Option[String])
  extends NumFunction(name, args, tipe, isConstant, resourceType)
{
  assert(tipe match {
    case PSimpleType("integer") => true
    case _ => false
  }, "The type of this int function is not an integer: "+this)
  assert(minValue <= maxValue, "Error: min value greater than max value in integer function: "+this)
}

case class FloatFunction(
                          override val name:String,
                          override val args:List[Argument],
                          override val tipe:PType,
                          override val isConstant:Boolean,
                          minValue:Float,
                          maxValue:Float,
                          override val resourceType:Option[String])
  extends NumFunction(name, args, tipe, isConstant, resourceType)
{
  assert(tipe match {
    case PSimpleType("float") => true
    case _ => false
  }, "The type of this float function is not a float: "+this)
  assert(minValue <= maxValue, "Error: min value greater than max value in float function: "+this)
}

class Constant(override val name :String, override val tipe:PType)
  extends Function(name, Nil, tipe, true) with DecompositionContent with ActionContent

case class TypeDecl(name:PSimpleType, parent:Option[PSimpleType], content:List[TypeContent]) extends AnmlBlock

case class Instance(tipe:PSimpleType, name:String) extends AnmlBlock


trait Annotation extends AnmlBlock with ActionContent
case class ObservationConditionsAnnotation(tp:Timepoint, conditions:List[DecompositionContent]) extends Annotation
case class TimepointTypeAnnotation(typ: String, tp:Timepoint) extends Annotation

object AnmlParser extends JavaTokenParsers {

  lazy val temporalAnnotation : Parser[TemporalAnnotation] = (
    annotationBase<~"contains" ^^ { case TemporalAnnotation(start, end, _) => TemporalAnnotation(start, end, "contains")}
      | annotationBase
    )

  /** A temporal annotation like `[all]` `[start, end]`, `[10, end]`, ...
    * The flag of this temporal annotation is set to "is" (it doesn't look for the "contains" keyword
    * after the annotation
    */
  lazy val annotationBase : Parser[TemporalAnnotation] = (
    "["~"all"~"]" ^^
      (x => TemporalAnnotation(new RelativeTimepoint("start"), new RelativeTimepoint("end"), "is"))
      | "["~> repsep(timepoint,",") <~"]" ^^ {
      case List(tp) => TemporalAnnotation(tp, tp, "is")
      case List(tp1, tp2) => TemporalAnnotation(tp1, tp2, "is")
    })

  lazy val timepoint : Parser[RelativeTimepoint] = (
    opt(timepointRef)~("+"|"-")~decimalNumber ^^ {
      case tp~"+"~delta => RelativeTimepoint(tp, delta.toInt)
      case tp~"-"~delta => RelativeTimepoint(tp, - delta.toInt)
    }
      | decimalNumber ^^ { case x => RelativeTimepoint(None, x.toInt)}
      | timepointRef ^^ (x => RelativeTimepoint(Some(x), 0))
      | failure("illegal timepoint")
    )

  lazy val timepointRef : Parser[TimepointRef] =
    kwTempAnnot~"("~word<~")" ^^ { case kw~"("~id => ExtractedTimepoint(kw, id) } |
      rawTimepoint

  lazy val rawTimepoint : Parser[Timepoint] =
    ident ^^ (kw => Timepoint(kw))

  lazy val statement : Parser[Statement] = (
     word~":"~statementWithoutID ^^ {
        case id~":"~s => s match {
          case SingleTermStatement(e, "") => SingleTermStatement(e, id)
          case TwoTermsStatement(e1, o, e2, "") => TwoTermsStatement(e1, o, e2, id)
          case ThreeTermsStatement(e1, o1, e2, o2, e3, "") => ThreeTermsStatement(e1, o1, e2, o2, e3, id)
          case _ => throw new ANMLException("Problem while parsing: id was detected by statementWithoutID")
        }}
    | statementWithoutID
    )

  lazy val statementWithoutID : Parser[Statement] = (
      literal~op~literal~op~literal ^^ { case e1~o1~e2~o2~e3 => new ThreeTermsStatement(e1, o1, e2, o2, e3, "") }
    | literal~op~literal ^^ { case e1~o~e2 => new TwoTermsStatement(e1, o, e2, "") }
    | literal ^^ (e => new SingleTermStatement(e, ""))
    | "ordered"~"("~>rep1sep(statement,",")<~")" ^^ (l => new OrderedStatements(l, ""))
    | "unordered"~"("~>rep1sep(statement,",")<~")" ^^ (l => new UnorderedStatements(l, ""))
    )

  /** Temporal constraint between two time points. It is of the form:
    * `start(xx) < end + x`, `start = end -5`, ...
    */
  lazy val tempConstraint : Parser[TemporalConstraint] =
    timepointRef~opt(constantAddition)~("="|"<="|">="|"<"|">")~timepointRef~opt(constantAddition)<~";" ^^ {
      case tp1~d1~"<"~tp2~d2 => ReqTemporalConstraint(tp1, "<", tp2, d2.getOrElse(0)-d1.getOrElse(0))
      case tp1~d1~"="~tp2~d2 => ReqTemporalConstraint(tp1, "=", tp2, d2.getOrElse(0)-d1.getOrElse(0))
      case tp1~d1~"<="~tp2~d2 => ReqTemporalConstraint(tp1, "<", tp2, d2.getOrElse(0)-d1.getOrElse(0)+1)
      case tp1~d1~">"~tp2~d2 => ReqTemporalConstraint(tp2, "<", tp1, d1.getOrElse(0)-d2.getOrElse(0))
      case tp1~d1~">="~tp2~d2 => ReqTemporalConstraint(tp2, "<", tp1, d1.getOrElse(0)-d2.getOrElse(0)+1)
    } |
      timepointRef~":in"~timepointRef~"+"~"["~decimalNumber~","~decimalNumber~"]"<~";" ^^ {
        case dst~":in"~src~"+"~"["~min~","~max~"]" => ContingentConstraint(src,dst,min.toInt,max.toInt)
      }

  /** Any string of the form `+ 10`, `- 2`, ... Returns an integer*/
  def constantAddition[Int] = ("+"|"-")~decimalNumber ^^ {
    case "+"~num => num.toInt
    case "-"~num => - num.toInt
  }

  lazy val unchainedLiteral : Parser[Expr] = (
      decimalNumber ^^ { x => NumExpr(x.toFloat) }
    | "-"~>decimalNumber ^^ { x => NumExpr(-x.toFloat) }
    | word~opt(refArgs) ^^ {
        case f~None => VarExpr(f)
        case f~Some(args) => FuncExpr(VarExpr(f), args)
      }
    | "{"~>rep1sep(literal, ",")<~"}" ^^ {
        case l => SetExpr(l.toSet)
      }
  )

  lazy val literal : Parser[Expr] =
    rep1sep(unchainedLiteral,".") ^^ {
      case List(variable) => variable
      case l => l.tail.foldLeft(l.head)((acc,cur) => ChainedExpr(acc, cur))
    }

  /** a var expr is a single word such as x, prettyLongName, ... */
  lazy val varExpr : Parser[VarExpr] =
    word ^^ (x => VarExpr(x))

  /** List of variable expression such as (x, y, z) */
  lazy val refArgs : Parser[List[Expr]] =
    "("~>repsep(literal, ",")<~")"

  lazy val action : Parser[Action] =
    "action"~>word~"("~repsep(argument, ",")~")"~actionBody ^^
      { case name~"("~args~")"~body => new Action(name, args, body)}

  lazy val actionBody : Parser[List[ActionContent]] = (
    "{"~>rep(actionContent)<~"}"~";" ^^ (_.flatten)
      | "{"~"}"~";" ^^ (x => List())
    )

  lazy val actionContent : Parser[List[ActionContent]] = (
    temporalStatements
      | decomposition ^^ (x => List(x))
      | tempConstraint ^^ (x => List(x))
      | annotation ^^ (x => List(x))
      | "motivated"~";" ^^^ List(Motivated)
      | "constant"~>typ~word<~";" ^^ {
      case tipe~name => List(new Constant(name, tipe)) }
      | "duration"~":="~>literal<~";" ^^ (x => List(ExactDuration(x)))
      | "duration"~":in"~"["~>literal~","~literal<~"]"~";" ^^ {
        case min~","~max => List(UncertainDuration(min, max))
      }
    )

  lazy val decomposition : Parser[Decomposition] =
    ":decomposition"~"{"~>rep(decompositionContent)<~"}"~";" ^^ {
      case content => Decomposition(content.flatten)
    }

  lazy val decompositionContent : Parser[List[DecompositionContent]] = (
    tempConstraint ^^ (x => List(x))
      | temporalStatements
      | "constant"~>typ~word<~";" ^^ {
      case tipe~name => List(new Constant(name, tipe))
    })


  lazy val argument : Parser[Argument] = (
    typ~word ^^ { case tipe~name => new Argument(tipe, name)}
      | failure("Argument malformed.")
    )

  lazy val temporalStatements : Parser[List[TemporalStatement]] = (
      temporalAnnotation~statements ^^ { case annot~statements => statements.map(new TemporalStatement(Some(annot), _))}
    | statementSemi ^^ { case s:Statement => List(new TemporalStatement(None, s))}
  )

  lazy val statementSemi : Parser[Statement] = statement<~";"

  lazy val statements : Parser[List[Statement]] = (
    "{"~>rep(statementSemi)<~"}"<~";"
      | statementSemi ^^ (x => List(x)))

  lazy val block : Parser[List[AnmlBlock]] = (
    action ^^ (a => List(a))
      | temporalStatements
      | tempConstraint ^^ (x => List(x))
      | functionDecl ^^ (func => List(func))
      | typeDecl ^^ (t => List(t))
      | instanceDecl
      | forallBlock ^^ (t => List(t))
      | annotation ^^ (t => List(t))
    )

  lazy val annotation : Parser[Annotation] =
    "::("~>observationConditions<~")" |
  "::("~>word~"("~rawTimepoint<~")"~")" ^^ {
    case typ~"("~tp => TimepointTypeAnnotation(typ, tp) }

  lazy val observationConditions : Parser[ObservationConditionsAnnotation] =
    "observation_conditions"~"("~>rawTimepoint~")"~"{"~rep(decompositionContent)<~"}" ^^ {
      case tp~")"~"{"~content => ObservationConditionsAnnotation(tp, content.flatten)
    }

  lazy val forallBlock : Parser[ForAll] =
    "forall"~"("~>rep1sep(argument,",")~")"~"{"~decompositionContent<~"}"~";" ^^ {
      case args~")"~"{"~content => ForAll(args, content)
    }

  lazy val anml : Parser[List[AnmlBlock]] = rep(block) ^^ (blockLists => blockLists.flatten)

  lazy val argList : Parser[List[Argument]] =
    "("~>repsep(argument,",")<~")"

  lazy val functionDecl : Parser[Function] = numFunctionDecl | symFunctionDecl

  lazy val symFunctionDecl : Parser[SymFunction] = (
    "constant"~>typ~word~opt(argList)<~";" ^^ {
      case t~name~Some(args) => SymFunction(name, args, t, isConstant=true)
      case t~name~None => SymFunction(name, Nil, t, isConstant=true)
    }
      | "fluent"~>anySymType~word~opt(argList)<~";" ^^ {
        case t~name~optArgs => SymFunction(name, optArgs.getOrElse(Nil), t, isConstant = false) }
      | "variable"~>anySymType~word<~";" ^^ {case t~name => SymFunction(name, List(), t, isConstant=false)}
      | "function"~>anySymType~word~argList<~";" ^^ {case t~name~args => SymFunction(name, args, t, isConstant=false)}
      | "predicate"~>word~argList<~";" ^^ {case name~args => SymFunction(name, args, PSimpleType("boolean"), isConstant=false)}
    )

  lazy val numFunctionDecl : Parser[NumFunction] = integerFunctionDecl | floatFunctionDecl

  lazy val numFunctionType : Parser[String] = "constant" | "variable" | "function" | "consumable" | "producible" | "reusable" | "replenishable"

  lazy val floatInterval : Parser[(Float, Float)] = (
    "["~>floatingPointNumber~","~floatingPointNumber<~"]" ^^ { case min~","~max => (min.toFloat, max.toFloat) }
      | "["~>floatingPointNumber~","~"infinity"<~"]" ^^ { case min~","~_ => (min.toFloat, Float.MaxValue) })

  lazy val intInterval : Parser[(Int, Int)] = (
    "["~>floatingPointNumber~","~floatingPointNumber<~"]" ^^ { case min~","~max => (min.toInt, max.toInt) }
      | "["~>floatingPointNumber~","~"infinity"<~"]" ^^ { case min~","~_ => (min.toInt, Int.MaxValue) })

  lazy val floatFunctionDecl : Parser[FloatFunction] =
    numFunctionType~"float"~opt(floatInterval)~word~opt(argList)<~";" ^^ {
      case fType~"float"~interval~name~argsOpt => {
        val resourceType =
          if(fType == "constant" || fType == "variable" || fType == "function") None
          else Some(fType)
        val constraints = interval match {
          case None => (Float.MinValue, Float.MaxValue)
          case Some(anInterval) => anInterval
        }
        val args = argsOpt match {
          case None => List()
          case Some(arguments) => arguments
        }
        new FloatFunction(name, args, PSimpleType("float"), fType == "constant", constraints._1, constraints._2, resourceType)
      }
    }

  lazy val integerFunctionDecl : Parser[IntFunction] =
    numFunctionType~"integer"~opt(intInterval)~word~opt(argList)<~";" ^^ {
      case fType~"integer"~interval~name~argsOpt => {
        val resourceType =
          if(fType == "constant" || fType == "variable" || fType == "function") None
          else Some(fType)
        val constraints = interval match {
          case None => (Int.MinValue, Int.MaxValue)
          case Some(anInterval) => anInterval
        }
        val args = argsOpt match {
          case None => List()
          case Some(arguments) => arguments
        }
        new IntFunction(name, args, PSimpleType("integer"), fType == "constant", constraints._1, constraints._2, resourceType)
      }
    }


  lazy val simpleSymType : Parser[PSimpleType] =
    (symType | word) ^^ { case t => PSimpleType(t)} |
      failure("Unable to parse type")

  lazy val anySymType : Parser[PType] =
    "("~>rep1sep(simpleType, "or")<~")" ^^ { case l => PDisjunctiveType(l.toSet) } |
      simpleType

  lazy val simpleType : Parser[PSimpleType] =
    (kwType | word) ^^ { case t => PSimpleType(t)} |
      failure("Unable to parse type")

  lazy val typ : Parser[PType] =
    "("~>rep1sep(simpleType, "or")<~")" ^^ { case l => PDisjunctiveType(l.toSet) } |
    simpleType

  lazy val typeDecl : Parser[TypeDecl] = (
    "type"~>simpleType~"<"~simpleType~"with"~typeBody<~";" ^^ {case name~"<"~parent~"with"~content => TypeDecl(name, Some(parent), content)}
      | "type"~>simpleType~"with"~typeBody<~";" ^^ {case name~"with"~content => TypeDecl(name, None, content)}
      | "type"~>simpleType~"<"~simpleType<~";" ^^ {case name~"<"~parent => TypeDecl(name, Some(parent), List())}
      | "type"~>simpleType<~";" ^^ (name => TypeDecl(name, None, List()))
      | failure("Not a valid type declaration")
    )

  lazy val typeBody : Parser[List[TypeContent]] = "{"~>rep(functionDecl)<~"}"

  lazy val instanceDecl : Parser[List[Instance]] =
    "instance"~>simpleType~repsep(word,",")<~";" ^^ {
      case tipe~names => names.map(Instance(tipe, _))
    }

  /** all predefined types: boolean, float, integer, object */
  lazy val kwType : Parser[String] = numType | symType

  /** predefined symbolic types: boolean, object */
  lazy val symType : Parser[String] = "boolean\\b".r// | "object"

  /** Predefined numeric types: float and integer */
  lazy val numType : Parser[String] = "float\\b".r | "integer\\b".r

  lazy val kwTempAnnot : Parser[String] = "start\\b".r | "end\\b".r

  lazy val keywords = (kwType | kwTempAnnot | "motivated\\b".r | "duration\\b".r | "ordered\\b".r | "unordered\\b".r)

  lazy val word = not(keywords) ~> ident
//  lazy val word = ident

  lazy val op : Parser[Operator] = opString ^^ { case op:String => Operator(op) }
  private def opString : Parser[String] =
    "==" | ":=" | ":->" | ":produce" | ":consume" | ":use" | "<" | "<=" | ">=" | ">" | "!=" | "in\\b".r

}


