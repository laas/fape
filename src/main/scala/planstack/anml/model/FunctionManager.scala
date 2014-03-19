package planstack.anml.model

import planstack.anml.{ANMLException, parser}

import scala.collection.mutable
import collection.JavaConversions._

class Function(val name:String, val valueType:String, val argTypes:List[String]) {
  def isConstant = false
}

class ConstFunction(name:String, valueType:String, argTypes:List[String])
  extends Function(name, valueType, argTypes)
{
  override def isConstant = true

  var values = List[Pair[List[String], String]]()

  def addValue(args:List[String], value:String) {
    val valuePair = (args, value)
    assert(values.forall(_._1 != args), "Function %s already contains a value for arguments %s".format(name, args))
    values = valuePair :: values
  }
}

class FunctionManager {

  /**
   * Maps function name to definition.
   * Those function have an implicit time parameter which is dealt with externally
   */
  private val functions = mutable.Map[String, Function]()

  /**
   * Maps from function name to definition of functions that do
   * not vary over time
   */
  private val constFunction = mutable.Map[String, ConstFunction]()

  def addFunction(f:parser.Function) {
    addFunction(f.name, f.tipe, f.args.map(_.tipe), f.isConstant)
  }

  /**
   * Adds a function declared in the scope of a type.
   *
   * For instance a function location(Loc a) declared in a type robot results in
   * a function Robot.location(Robot r, Loc a)
   *
   * @param scope Name of the type in which the function is declared
   * @param f function definition
   */
  def addScopedFunction(scope:String, f:parser.Function) {
    addFunction(scope+"."+f.name, f.tipe, scope :: f.args.map(_.tipe), f.isConstant)
  }

  def addFunction(name:String, valueType:String, argTypes:List[String], isConstant:Boolean) {
    if(isConstant) {
      assert(!constFunction.contains(name))
      constFunction(name) = new ConstFunction(name, valueType, argTypes)
    } else {
      assert(!functions.contains(name), "Function "+name+" already exists.")
      functions(name) = new Function(name, valueType, argTypes)
    }
  }

  @deprecated
  def addConstFuncValue(funcName:String, args:List[String], value:String) {
    assert(constFunction.contains(funcName))
    constFunction(funcName).addValue(args, value)
  }

  def isDefined(funcName:String) = constFunction.contains(funcName) || functions.contains(funcName)

  def isConstantFunc(funcName:String) = constFunction.contains(funcName)

  def getAll : Seq[Function] = {
    (functions.values ++ constFunction.values).toList
  }

  def jGetAll = seqAsJavaList(getAll)

  def get(functionName:String) = {
    if(functions.contains(functionName))
      functions(functionName)
    else if(constFunction.contains(functionName))
      constFunction(functionName)
    else
      throw new ANMLException("Unknown function name: "+functionName)
  }
}
