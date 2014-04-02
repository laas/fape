package planstack.anml.model

import planstack.anml.{ANMLException, parser}

import scala.collection.mutable
import collection.JavaConversions._

/** Representation of an ANML function.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  */
class Function(val name:String, val valueType:String, val argTypes:List[String]) {
  /** True if this function is defined as constant. False otherwise */
  val isConstant = false
}

/** Representation of an ANML constant function.
 *
 * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
 *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
 * @param valueType Type of the function's value.
 * @param argTypes Types of the arguments of the function in order.
 */
class ConstFunction(name:String, valueType:String, argTypes:List[String])
  extends Function(name, valueType, argTypes)
{
  override val isConstant = true
}

/** Storage for functions found in an [[planstack.anml.model.AnmlProblem]] */
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

  /** Look up if there exists a function with this name.
    *
    * @param funcName Name of the function to look up.
    * @return True if such a function exists, False otherwise.
    */
  def isDefined(funcName:String) = constFunction.contains(funcName) || functions.contains(funcName)

  /** Returns true if the given function name maps to a constant function
    *
    * @param funcName Name of the function to look up.
    * @return True if the function is constant, False otherwise.
    */
  def isConstantFunc(funcName:String) = constFunction.contains(funcName)

  /** Returns all functions stored in this function manager */
  def getAll : java.util.List[Function] = seqAsJavaList((functions.values ++ constFunction.values).toList)

  /** Finds the definition of the function with the given name.
    *
    * @param functionName Name of the function to look up.
    * @return The function definition. Throws an [[planstack.anml.ANMLException]] if no such function can be found.
    */
  def get(functionName:String) = {
    if(functions.contains(functionName))
      functions(functionName)
    else if(constFunction.contains(functionName))
      constFunction(functionName)
    else
      throw new ANMLException("Unknown function name: "+functionName)
  }
}
