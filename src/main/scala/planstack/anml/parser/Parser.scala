package planstack.anml.parser

import java.io.FileReader
import scala.util.parsing.combinator._


sealed trait AnmlBlock
sealed trait ActionContent
sealed trait TypeContent




case class TemporalStatement(annotation:TemporalAnnotation, statement:Statement) extends AnmlBlock with ActionContent

case class TemporalAnnotation(start:RelativeTimepoint, end:RelativeTimepoint)

case class RelativeTimepoint(tp:String, delta:Int)


sealed abstract class Statement(val variable:Expr)

case class Assignment(left:Expr, right:VarExpr) extends Statement(left)

case class Transition(left:Expr, from:VarExpr, to:VarExpr) extends Statement(left)

case class Persistence(left:Expr, value:VarExpr) extends Statement(left)



case class Action(name:String, args:List[Argument], content:List[ActionContent]) extends AnmlBlock

case class Argument(tipe:String, name:String)

case class Decomposition(actions:PartiallyOrderedActionRef) extends ActionContent

sealed trait PartiallyOrderedActionRef {
  def contained : Set[ActionRef]
}

case class ActionRef(name:String, args:List[Expr]) extends PartiallyOrderedActionRef {
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



case class Function(name:String, args:List[Argument], tipe:String, const:Boolean) extends AnmlBlock with TypeContent

case class Type(name:String, parent:String, content:List[TypeContent]) extends AnmlBlock

case class Instance(tipe:String, name:String) extends AnmlBlock

object Comment extends AnmlBlock

object AnmlParser extends JavaTokenParsers {
  def annotation : Parser[TemporalAnnotation] = (
      "["~"all"~"]" ^^
        (x => TemporalAnnotation(RelativeTimepoint("start",0), RelativeTimepoint("end",0)))
      | "["~> repsep(timepoint,",") <~"]" ^^ {
        case List(tp) => TemporalAnnotation(tp, tp)
        case List(tp1, tp2) => TemporalAnnotation(tp1, tp2)
    })

  def timepoint : Parser[RelativeTimepoint] = (
      ("start" | "end") ^^ (x => RelativeTimepoint(x, 0))
    | failure("illegal timepoint, adding values is not supported yet")
    )

  def statement : Parser[Statement] = (
      expr~"=="~varExpr<~";" ^^
        {case sv~"=="~value => Persistence(sv, value)}
    | expr~":="~varExpr<~";" ^^
        {case sv~":="~value => Assignment(sv, value)}
    | expr~"=="~varExpr~":->"~varExpr<~";" ^^
        {case sv~"=="~from~":->"~to => Transition(sv, from, to)}
    )

  def expr : Parser[Expr] =
      repsep(ident,".")~opt(refArgs) ^^ {
        case List(variable) ~ None => VarExpr(variable)
        case svExpr~None => FuncExpr(svExpr, Nil)
        case svExpr~Some(args) => FuncExpr(svExpr, args)
      }

  def varExpr : Parser[VarExpr] =
      ident ^^ (x => VarExpr(x))

  def refArgs : Parser[List[VarExpr]] = (
      "("~>repsep(varExpr, ",")<~")"
    )

  def action : Parser[Action] =
      "action"~>ident~"("~repsep(argument, ",")~")"~actionBody ^^
        { case name~"("~args~")"~body => new Action(name, args, body)}

  def actionBody : Parser[List[ActionContent]] = (
      "{"~>rep(actionContent)<~"}"~";" ^^ (_.flatten)
    | "{"~"}"~";" ^^ (x => List())
    )

  def actionContent : Parser[List[ActionContent]] = (
      temporalStatements
    | decomposition ^^ (x => List(x))
  )

  def decomposition : Parser[Decomposition] =
      ":decomposition"~"{"~>partiallyOrderedActionRef<~"}"~";" ^^ {
        case poActions => Decomposition(poActions)
      }

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
      ident~"("~repsep(expr, ",")<~")" ^^ {
        case name~"("~args => ActionRef(name, args)
      }

  def argument : Parser[Argument] = (
      tipe~ident ^^ { case tipe~name => new Argument(tipe, name)}
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
      "constant"~>tipe~ident~opt(argList)<~";" ^^ {
        case t~name~Some(args) => Function(name, args, t, true)
        case t~name~None => Function(name, Nil, t, true)
      }
    | "variable"~>tipe~ident<~";" ^^ {case t~name => Function(name, List(), t, false)}
    | "function"~>tipe~ident~argList<~";" ^^ {case t~name~args => Function(name, args, t, false)}
    | "predicate"~>ident~argList<~";" ^^ {case name~args => Function(name, args, "boolean", false)}
    )

  def tipe : Parser[String] =
      "float" | ident | failure("Unable to parse type")

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
      "instance"~>tipe~repsep(ident,",")<~";" ^^ {
        case tipe~names => names.map(Instance(tipe, _))
      }

  def comment : Parser[AnmlBlock] = """/\\*(?:.|[\n\r])*?\\*/""".r ^^^ Comment
}

