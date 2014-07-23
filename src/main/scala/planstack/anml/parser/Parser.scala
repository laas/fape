package planstack.anml.parser

import java.io.FileReader
import scala.util.parsing.combinator._
import planstack.anml.model.AnmlProblem


sealed trait AnmlBlock
sealed trait ActionContent
sealed trait DecompositionContent
sealed trait TypeContent




case class TemporalStatement(annotation:TemporalAnnotation, statement:Statement) extends AnmlBlock with ActionContent with DecompositionContent

case class TemporalAnnotation(start:RelativeTimepoint, end:RelativeTimepoint, flag:String) {
  require(flag == "is" || flag == "contains")
}

case class RelativeTimepoint(tp:Option[TimepointRef], delta:Int) {
  def this(str:String) = this(Some(new TimepointRef(str)), 0)
  def this(abs:Int) = this(None, abs)
}


case class TimepointRef(extractor:String, id:String) {
  def this(extractor:String) = this(extractor, "")
}


sealed abstract class Statement(val variable:Expr, val id:String)

/** Logical statement such as assignment, transition and persistence */
sealed abstract class LogStatement(variable:Expr, id:String) extends Statement(variable, id)

case class Assignment(left:Expr, right:VarExpr, override val id:String) extends LogStatement(left, id)

case class Transition(left:Expr, from:VarExpr, to:VarExpr, override val id:String) extends LogStatement(left, id)

case class Persistence(left:Expr, value:VarExpr, override val id:String) extends LogStatement(left, id)


sealed abstract class ResourceStatement(variable:Expr, id:String) extends Statement(variable, id)

/** resourceFunction := 23; */
case class SetResource(left :Expr, right:Float, override val id :String) extends ResourceStatement(left, id)

case class ProduceResource(left :Expr, right :Float, override val id :String) extends ResourceStatement(left, id)

case class RequireResource(left :Expr, operator :String, right :Float, override val id :String) extends ResourceStatement(left, id)

case class LendResource(left :Expr, right :Float, override val id :String) extends ResourceStatement(left, id)

case class UseResource(left :Expr, right :Float, override val id :String) extends ResourceStatement(left, id)

case class ConsumeResource(left :Expr, right :Float, override val id :String) extends ResourceStatement(left, id)





case class Action(name:String, args:List[Argument], content:List[ActionContent]) extends AnmlBlock

object Motivated extends ActionContent

case class Argument(tipe:String, name:String)

case class Decomposition(content:Seq[DecompositionContent]) extends ActionContent

sealed trait PartiallyOrderedActionRef extends DecompositionContent {
  def contained : Set[ActionRef]
}

case class ActionRef(name:String, args:List[Expr], id:String) extends PartiallyOrderedActionRef {
  def contained = Set(this)
}
case class OrderedActionRef(list:List[PartiallyOrderedActionRef]) extends PartiallyOrderedActionRef {
  def contained = list.foldLeft(Set[ActionRef]())((set, x) => set ++ x.contained )
}
case class UnorderedActionRef(list:List[PartiallyOrderedActionRef]) extends PartiallyOrderedActionRef {
  def contained = list.foldLeft(Set[ActionRef]())((set, x) => set ++ x.contained )
}

sealed trait Expr {
  def functionName : String
}
case class VarExpr(variable:String) extends Expr {
  override def functionName = variable
}
case class FuncExpr(svExpr:List[String], args:List[VarExpr]) extends Expr {
  override def functionName = svExpr.mkString(".")
}

case class TemporalConstraint(tp1:TimepointRef, operator:String, tp2:TimepointRef, delta:Int)
  extends DecompositionContent with ActionContent with AnmlBlock {
  require(operator == "=" || operator == "<")
}

class Function(val name:String, val args:List[Argument], val tipe:String, val isConstant:Boolean) extends AnmlBlock with TypeContent

case class SymFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:String,
    override val isConstant:Boolean)
  extends Function(name, args, tipe, isConstant)
{
  assert(tipe != "integer" && tipe != "float", "Symbolic function with a numeric type: "+this)
}

class NumFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:String,
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
    override val tipe:String,
    override val isConstant:Boolean,
    minValue:Int,
    maxValue:Int,
    override val resourceType:Option[String])
  extends NumFunction(name, args, tipe, isConstant, resourceType)
{
  assert(tipe == "integer", "The type of this int function is not an integer: "+this)
  assert(minValue <= maxValue, "Error: min value greater than max value in integer function: "+this)
}

case class FloatFunction(
    override val name:String,
    override val args:List[Argument],
    override val tipe:String,
    override val isConstant:Boolean,
    minValue:Float,
    maxValue:Float,
    override val resourceType:Option[String])
  extends NumFunction(name, args, tipe, isConstant, resourceType)
{
  assert(tipe == "float", "The type of this float function is not a float: "+this)
  assert(minValue <= maxValue, "Error: min value greater than max value in float function: "+this)
}

class Constant(override val name :String, override val tipe:String) extends Function(name, Nil, tipe, true) with DecompositionContent

case class Type(name:String, parent:String, content:List[TypeContent]) extends AnmlBlock

case class Instance(tipe:String, name:String) extends AnmlBlock

object AnmlParser extends JavaTokenParsers {

  def annotation : Parser[TemporalAnnotation] = (
      annotationBase<~"contains" ^^ { case TemporalAnnotation(start, end, _) => TemporalAnnotation(start, end, "contains")}
    | annotationBase
    )

  /** A temporal annotation like `[all]` `[start, end]`, `[10, end]`, ...
    * The flag of this temporal annotation is set to "is" (it doesn't look for the "contains" keyword
    * after the annotation
    */
  def annotationBase : Parser[TemporalAnnotation] = (
      "["~"all"~"]" ^^
        (x => TemporalAnnotation(new RelativeTimepoint("start"), new RelativeTimepoint("end"), "is"))
    | "["~> repsep(timepoint,",") <~"]" ^^ {
        case List(tp) => TemporalAnnotation(tp, tp, "is")
        case List(tp1, tp2) => TemporalAnnotation(tp1, tp2, "is")
      })

  def timepoint : Parser[RelativeTimepoint] = (
      opt(timepointRef)~("+"|"-")~decimalNumber ^^ {
        case tp~"+"~delta => RelativeTimepoint(tp, delta.toInt)
        case tp~"-"~delta => RelativeTimepoint(tp, - delta.toInt)
      }
    | decimalNumber ^^ { case x => RelativeTimepoint(None, x.toInt)}
    | timepointRef ^^ (x => RelativeTimepoint(Some(x), 0))
    | failure("illegal timepoint")
    )

  def timepointRef : Parser[TimepointRef] = (
      kwTempAnnot~"("~word<~")" ^^ {
        case kw~"("~id => TimepointRef(kw, id) }
    | kwTempAnnot ^^
        (kw => TimepointRef(kw, ""))
    )

  def statement : Parser[Statement] = (
      word~":"~statementWithoutId ^^ {
        case id~":"~Persistence(sv, value, _) => Persistence(sv, value, id)
        case id~":"~Assignment(sv, value, _) => Assignment(sv, value, id)
        case id~":"~Transition(sv, from, to, _) => Transition(sv, from, to, id)
        case id~":"~SetResource(sv, param, _) => SetResource(sv, param, id)
        case id~":"~ConsumeResource(sv, param, _) => ConsumeResource(sv, param, id)
        case id~":"~ProduceResource(sv, param, _) => ProduceResource(sv, param, id)
        case id~":"~UseResource(sv, param, _) => UseResource(sv, param, id)
        case id~":"~LendResource(sv, param, _) => LendResource(sv, param, id)
        case id~":"~RequireResource(sv, op, param, _) => RequireResource(sv, op, param, id)
      }
    | statementWithoutId
    )

  def statementWithoutId : Parser[Statement] = resourceStatementWithoutID | logStatementWithoutId

  def resourceStatementWithoutID : Parser[ResourceStatement] = (
      expr~":="~floatingPointNumber<~";" ^^ {
        case left~":="~floatVal => SetResource(left, floatVal.toFloat, "")}
    | expr~("<="|">="|"<"|">")~floatingPointNumber<~";" ^^ {
        case left~operator~floatVal => RequireResource(left, operator, floatVal.toFloat, "")}
    | expr~":consume"~floatingPointNumber<~";" ^^ {
        case left~":consume"~floatVal => ConsumeResource(left, floatVal.toFloat, "")}
    | expr~":produce"~floatingPointNumber<~";" ^^ {
        case left~":produce"~floatVal => ProduceResource(left, floatVal.toFloat, "")}
    | expr~":use"~floatingPointNumber<~";" ^^ {
        case left~":use"~floatVal => UseResource(left, floatVal.toFloat, "")}
    | expr~":lend"~floatingPointNumber<~";" ^^ {
        case left~":lend"~floatVal => LendResource(left, floatVal.toFloat, "")}
    )

  def logStatementWithoutId : Parser[LogStatement] = (
      expr~"=="~varExpr<~";" ^^
        {case sv~"=="~value => Persistence(sv, value, "")}
    | expr~":="~varExpr<~";" ^^
        {case sv~":="~value => Assignment(sv, value, "")}
    | expr~"=="~varExpr~":->"~varExpr<~";" ^^
        {case sv~"=="~from~":->"~to => Transition(sv, from, to, "")}
    )

  /** Temporal constraint between two time points. It is of the form:
    * `start(xx) < end + x`, `start = end -5`, ...
    */
  def tempConstraint : Parser[TemporalConstraint] =
      timepointRef~("<"|"=")~timepointRef~opt(constantAddition)<~";" ^^ {
        case tp1~op~tp2~None => TemporalConstraint(tp1, op, tp2, 0)
        case tp1~op~tp2~Some(delta) => TemporalConstraint(tp1, op, tp2, delta)
      }

  /** Any string of the form `+ 10`, `- 2`, ... Returns an integer*/
  def constantAddition[Int] = ("+"|"-")~decimalNumber ^^ {
    case "+"~num => num.toInt
    case "-"~num => - num.toInt
  }

  def expr : Parser[Expr] =
      repsep(word,".")~opt(refArgs) ^^ {
        case List(variable) ~ None => VarExpr(variable)
        case svExpr~None => FuncExpr(svExpr, Nil)
        case svExpr~Some(args) => FuncExpr(svExpr, args)
      }

  /** a var expr is a single word such as x, prettyLongName, ... */
  def varExpr : Parser[VarExpr] =
      word ^^ (x => VarExpr(x))

  /** List of variable expression such as (x, y, z) */
  def refArgs : Parser[List[VarExpr]] =
      "("~>repsep(varExpr, ",")<~")"

  def action : Parser[Action] =
      "action"~>word~"("~repsep(argument, ",")~")"~actionBody ^^
        { case name~"("~args~")"~body => new Action(name, args, body)}

  def actionBody : Parser[List[ActionContent]] = (
      "{"~>rep(actionContent)<~"}"~";" ^^ (_.flatten)
    | "{"~"}"~";" ^^ (x => List())
    )

  def actionContent : Parser[List[ActionContent]] = (
      temporalStatements
    | decomposition ^^ (x => List(x))
    | tempConstraint ^^ (x => List(x))
    | "motivated"~";" ^^^ List(Motivated)
  )

  def decomposition : Parser[Decomposition] =
      ":decomposition"~"{"~>rep(decompositionContent)<~"}"~";" ^^ {
        case content => Decomposition(content.flatten)
      }

  def decompositionContent : Parser[List[DecompositionContent]] = (
      tempConstraint ^^ (x => List(x))
    | temporalStatements
    | partiallyOrderedActionRef<~";" ^^ (x => List(x))
    | "constant"~>(word|kwType)~word<~";" ^^ {
        case tipe~name => List(new Constant(name, tipe))
      })


  def partiallyOrderedActionRef : Parser[PartiallyOrderedActionRef] = (
      "ordered"~"("~>repsep(partiallyOrderedActionRef,",")<~")" ^^ {
        case poActions => OrderedActionRef(poActions)
      }
    | "unordered"~"("~>repsep(partiallyOrderedActionRef,",")<~")" ^^ {
        case poActions => UnorderedActionRef(poActions)
      }
    | actionRef
    )

  def actionRef : Parser[ActionRef] =
      opt(word<~":")~word~"("~repsep(expr, ",")<~")" ^^ {
        case Some(id)~name~"("~args => ActionRef(name, args, id)
        case None~name~"("~args => ActionRef(name, args, "")
      }

  def argument : Parser[Argument] = (
      tipe~word ^^ { case tipe~name => new Argument(tipe, name)}
    | failure("Argument malformed.")
    )

  def temporalStatements : Parser[List[TemporalStatement]] =
      annotation~statements ^^ { case annot~statements => statements.map(new TemporalStatement(annot, _))}

  def statements : Parser[List[Statement]] = (
      "{"~>rep(statement)<~"}"<~";"
    | statement ^^ (x => List(x)))

  def block : Parser[List[AnmlBlock]] = (
      action ^^ (a => List(a))
    | temporalStatements
    | tempConstraint ^^ (x => List(x))
    | functionDecl ^^ (func => List(func))
    | typeDecl ^^ (t => List(t))
    | instanceDecl
    )

  def anml : Parser[List[AnmlBlock]] = rep(block) ^^ (blockLists => blockLists.flatten)

  def argList : Parser[List[Argument]] =
      "("~>repsep(argument,",")<~")"

  def functionDecl : Parser[Function] = numFunctionDecl | symFunctionDecl

  def anySymType : Parser[String] = symType | word

  def symFunctionDecl : Parser[SymFunction] = (
      "constant"~>anySymType~word~opt(argList)<~";" ^^ {
        case t~name~Some(args) => SymFunction(name, args, t, isConstant=true)
        case t~name~None => SymFunction(name, Nil, t, isConstant=true)
      }
    | "variable"~>anySymType~word<~";" ^^ {case t~name => SymFunction(name, List(), t, isConstant=false)}
    | "function"~>anySymType~word~argList<~";" ^^ {case t~name~args => SymFunction(name, args, t, isConstant=false)}
    | "predicate"~>anySymType~argList<~";" ^^ {case name~args => SymFunction(name, args, "boolean", isConstant=false)}
    )

  def numFunctionDecl : Parser[NumFunction] = integerFunctionDecl | floatFunctionDecl

  def numFunctionType : Parser[String] = "constant" | "variable" | "function" | "consumable" | "producible" | "reusable" | "replenishable"

  def floatInterval : Parser[(Float, Float)] = (
      "["~>floatingPointNumber~","~floatingPointNumber<~"]" ^^ { case min~","~max => (min.toFloat, max.toFloat) }
    | "["~>floatingPointNumber~","~"infinity"<~"]" ^^ { case min~","~_ => (min.toFloat, Float.MaxValue) })

  def intInterval : Parser[(Int, Int)] = (
      "["~>floatingPointNumber~","~floatingPointNumber<~"]" ^^ { case min~","~max => (min.toInt, max.toInt) }
    | "["~>floatingPointNumber~","~"infinity"<~"]" ^^ { case min~","~_ => (min.toInt, Int.MaxValue) })

  def floatFunctionDecl : Parser[FloatFunction] =
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
          new FloatFunction(name, args, "float", fType == "constant", constraints._1, constraints._2, resourceType)
        }
      }

  def integerFunctionDecl : Parser[IntFunction] =
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
          new IntFunction(name, args, "integer", fType == "constant", constraints._1, constraints._2, resourceType)
        }
      }


  def tipe : Parser[String] =
      kwType | word | failure("Unable to parse type")

  def typeDecl : Parser[Type] = (
      "type"~>tipe~"<"~tipe~"with"~typeBody<~";" ^^ {case name~"<"~parent~"with"~content => Type(name, parent, content)}
    | "type"~>tipe~"with"~typeBody<~";" ^^ {case name~"with"~content => Type(name, "", content)}
    | "type"~>tipe~"<"~tipe<~";" ^^ {case name~"<"~parent => Type(name, parent, List())}
    | "type"~>tipe<~";" ^^ (name => Type(name, "", List()))
    | failure("Not a valid type declaration")
    )

  def typeBody : Parser[List[TypeContent]] = "{"~>rep(functionDecl)<~"}"

  def instanceDecl : Parser[List[Instance]] =
      "instance"~>tipe~repsep(word,",")<~";" ^^ {
        case tipe~names => names.map(Instance(tipe, _))
      }

  /** all predefined types: boolean, float, integer, object */
  def kwType : Parser[String] = numType | symType

  /** predefined symbolic types: boolean, object */
  def symType : Parser[String] = "boolean" | "object"

  /** Predefined numeric types: float and integer */
  def numType : Parser[String] = "float" | "integer"

  def kwTempAnnot : Parser[String] = "start" | "end"

  def keywords = kwType | kwTempAnnot

  def word = not(keywords) ~> ident
}



object Test extends App {
  import AnmlParser._
  /*
  parseAll(temporalStatements, new FileReader("resources/test-resources.anml")) match {
    case Success(res, _) => {
      val tmp = res
      val x = 0
    }
    case x => println(x)
  }
*/
  if(args.size < 1) {
    println("Error: give one anml file as argument.")
  } else {
    val pb = new AnmlProblem
    pb.addAnml(ANMLFactory.parseAnmlFromFile(args(0)))

    val x = 0;
  }



}