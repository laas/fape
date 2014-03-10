package planstack.anml.model

import planstack.anml.{ANMLException, parser}

import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import planstack.anml.parser._
import planstack.anml.parser.FuncExpr
import planstack.anml.parser.TemporalStatement
import planstack.anml.parser.VarExpr
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


class AnmlProblem {


  val instances = new InstanceManager
  val functions = new FunctionManager
  val context = new Context(None)

  val absStatements = ListBuffer[AbstractTemporalStatement]()
  val actions = ListBuffer[AbstractAction]()

  def expressionToValue(expr:Expr) : String = {
    expr match {
      case v:VarExpr => {
        if(instances.containsInstance(v.variable))
          v.variable
        else
          throw new ANMLException("Unknown instance: "+v.variable)
      }
      case x => throw new ANMLException("Cannot find value for expression "+expr)
    }
  }


  def addAnmlBlocks(blocks:Seq[parser.AnmlBlock]) {

    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      instances.addType(typeDecl.name, typeDecl.parent)
    })

    blocks.filter(_.isInstanceOf[parser.Instance]).map(_.asInstanceOf[parser.Instance]) foreach(instanceDecl => {
      instances.addInstance(instanceDecl.name, instanceDecl.tipe)
    })

    for((name, tipe) <- instances.instances) {
      context.addVar(name, tipe, name)
    }

    blocks.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]) foreach(funcDecl => {
      functions.addFunction(funcDecl)
    })

    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      typeDecl.content.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]).foreach(scopedFunction => {
        functions.addScopedFunction(typeDecl.name, scopedFunction)
        instances.addMethodToType(typeDecl.name, scopedFunction.name)
      })
    })

    blocks.filter(_.isInstanceOf[parser.TemporalStatement]).map(_.asInstanceOf[parser.TemporalStatement]) foreach(tempStatement => {
      val ts = AbstractTemporalStatement(this, this.context, tempStatement)
      absStatements += ts

      println(ts)
    })

    blocks.filter(_.isInstanceOf[parser.Action]).map(_.asInstanceOf[parser.Action]) foreach(actionDecl => {
      actions +=  AbstractAction(actionDecl, this)
    })

  }

}
