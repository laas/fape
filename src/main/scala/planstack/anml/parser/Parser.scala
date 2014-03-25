package planstack.anml.parser

import java.io.FileReader
import scala.util.parsing.combinator._


sealed trait AnmlBlock
sealed trait ActionContent
sealed trait DecompositionContent
sealed trait TypeContent




case class TemporalStatement(annotation:TemporalAnnotation, statement:Statement) extends AnmlBlock with ActionContent

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

case class Assignment(left:Expr, right:VarExpr, override val id:String) extends Statement(left, id)

case class Transition(left:Expr, from:VarExpr, to:VarExpr, override val id:String) extends Statement(left, id)

case class Persistence(left:Expr, value:VarExpr, override val id:String) extends Statement(left, id)



case class Action(name:String, args:List[Argument], content:List[ActionContent]) extends AnmlBlock

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

case class Function(name:String, args:List[Argument], tipe:String, isConstant:Boolean) extends AnmlBlock with TypeContent

case class Type(name:String, parent:String, content:List[TypeContent]) extends AnmlBlock

case class Instance(tipe:String, name:String) extends AnmlBlock

object Comment extends AnmlBlock

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
      }
    | statementWithoutId
    )

  def statementWithoutId : Parser[Statement] = (
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
      timepointRef~("<"|"=")~timepointRef~opt(constantAddition) ^^ {
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
  )

  def decomposition : Parser[Decomposition] =
      ":decomposition"~"{"~>rep(decompositionContent<~";")<~"}"~";" ^^ {
        case content => Decomposition(content)
      }

  def decompositionContent : Parser[DecompositionContent] =
      tempConstraint | partiallyOrderedActionRef

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
    | functionDecl ^^ (func => List(func))
    | typeDecl ^^ (t => List(t))
    | instanceDecl
    | comment ^^^ List()
    )

  def anml : Parser[List[AnmlBlock]] = rep(block) ^^ (blockLists => blockLists.flatten)

  def argList : Parser[List[Argument]] = (
      "("~>repsep(argument,",")<~")"
  )

  def functionDecl : Parser[Function] = (
      "constant"~>tipe~word~opt(argList)<~";" ^^ {
        case t~name~Some(args) => Function(name, args, t, isConstant=true)
        case t~name~None => Function(name, Nil, t, isConstant=true)
      }
    | "variable"~>tipe~word<~";" ^^ {case t~name => Function(name, List(), t, isConstant=false)}
    | "function"~>tipe~word~argList<~";" ^^ {case t~name~args => Function(name, args, t, isConstant=false)}
    | "predicate"~>word~argList<~";" ^^ {case name~args => Function(name, args, "boolean", isConstant=false)}
    )

  def tipe : Parser[String] =
      kwTypes | word | failure("Unable to parse type")

  def typeDecl : Parser[Type] = (
      "type"~>tipe~"<"~tipe~"with"~typeBody<~";" ^^ {case name~"<"~parent~"with"~content => Type(name, parent, content)}
    | "type"~>tipe~"with"~typeBody<~";" ^^ {case name~"with"~content => Type(name, "", content)}
    | "type"~>tipe~"<"~tipe<~";" ^^ {case name~"<"~parent => Type(name, parent, List())}
    | "type"~>tipe<~";" ^^ (name => Type(name, "", List()))
    | failure("Not a valid type declaration")
    )

  def typeBody : Parser[List[TypeContent]] = (
      "{"~>rep(functionDecl)<~"}"
    )

  def instanceDecl : Parser[List[Instance]] =
      "instance"~>tipe~repsep(word,",")<~";" ^^ {
        case tipe~names => names.map(Instance(tipe, _))
      }

  def comment : Parser[AnmlBlock] = """/\\*(?:.|[\n\r])*?\\*/""".r ^^^ Comment

  def kwTypes : Parser[String] = "float" | "boolean" | "object"
  def kwTempAnnot : Parser[String] = "start" | "end"

  def keywords = kwTypes | kwTempAnnot

  def word = not(keywords) ~> ident
}

