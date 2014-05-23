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
  * @param isConstant True if this function is defined as constant. False otherwise
  */
abstract class Function(val name:String, val valueType:String, val argTypes:List[String], val isConstant:Boolean) {

  /** Builds a version of this defined as if it was scoped inside this containingType.
    * if the function is `AType aFunction(Object)`, invoking `scoped("Container)` on it would result in:
    * `AType Container.aFunction(Container, Object)`
    *
    * @param containingType Type in which the function was declared.
    * @return THe scoped version of the function.
    */
  def scoped(containingType : String) : Function
}

/** Representation of an ANML symbolic function.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  */
class SymFunction(name:String, valueType:String, argTypes:List[String], isConstant:Boolean)
  extends Function(name, valueType, argTypes, isConstant)
{
  def scoped(containingType : String) : SymFunction =
    new SymFunction(containingType+"."+name, valueType, containingType::argTypes, isConstant)
}

/** Representation of an ANML numeric function (on float or integer).
  * See derived classes [[planstack.anml.model.IntFunction]] and [[planstack.anml.model.FloatFunction]]
  * for concrete implementations.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  * @param resourceType Type of the resource which can be one of consumable, replenishable, reusable or producible.
  *                     If it is an empty String, then it is a generic resource.
  */
abstract class NumFunction(name:String, valueType:String, argTypes:List[String], isConstant:Boolean, val resourceType:String)
  extends Function(name, valueType, argTypes, isConstant)
{
  assert(valueType == "integer" || valueType == "float")

  /** Returns true if this function has a specific resource type in consumable, replenishable, reusable or producible.
    * If this is the case, you should check `resourceType` to see what it is.
    */
  def hasSpecificResourceType : Boolean = resourceType != ""
}

/** Representation of an ANML function on integers.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  * @param minValue Minimum possible value of the function. It is set to `Integer.MIN_VALUE` if no lower bound is
  *                 specified in the ANML model.
  * @param maxValue Maximum possible value of the function. It is set to `Integer.MAX_VALUE` if no upper bound is
  *                 specified in the ANML model.
  * @param resourceType Type of the resource which can be one of consumable, replenishable, reusable or producible.
  *                     If it is an empty String, then it is a generic resource.
  */
class IntFunction(name:String, valueType:String, argTypes:List[String], isConstant:Boolean, val minValue:Int, val maxValue:Int, resourceType:String)
  extends NumFunction(name, valueType, argTypes, isConstant, resourceType)
{
  assert(valueType == "integer")

  def scoped(container : String) : IntFunction =
    new IntFunction(container+"."+name, valueType, container::argTypes, isConstant, minValue, maxValue, resourceType)
}

/** Representation of an ANML function on floats.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  * @param minValue Minimum possible value of the function. It is set to `Float.MIN_VALUE` if no lower bound is
  *                 specified in the ANML model.
  * @param maxValue Maximum possible value of the function. It is set to `Float.MAX_VALUE` if no upper bound is
  *                 specified in the ANML model.
  * @param resourceType Type of the resource which can be one of consumable, replenishable, reusable or producible.
  *                     If it is an empty String, then it is a generic resource.
  */
class FloatFunction(name:String, valueType:String, argTypes:List[String], isConstant:Boolean, val minValue:Float, val maxValue:Float, resourceType:String)
  extends NumFunction(name, valueType, argTypes, isConstant, resourceType)
{
  assert(valueType == "float")

  def scoped(container : String) : FloatFunction =
    new FloatFunction(container+"."+name, valueType, container::argTypes, isConstant, minValue, maxValue, resourceType)
}

/** Storage for functions found in an [[planstack.anml.model.AnmlProblem]] */
class FunctionManager {

  /**
   * Maps function name to definition.
   * Those function have an implicit time parameter which is dealt with externally
   */
  private val functions = mutable.Map[String, Function]()

  /** Converts a function from the parser model and adds it to this function manager. */
  def addFunction(f:parser.Function) {
    addFunction(buildFunction(f))
  }

  /** Builds a Function from the output of the ANML parser. */
  private def buildFunction(f : parser.Function) : Function = {
    f match {
      case parser.SymFunction(name, args, tipe, isConstant) =>
        new SymFunction(name, tipe, args.map(_.tipe), isConstant)
      case parser.IntFunction(name, args, tipe, isConstant, min, max, Some(resourceType)) => {
        new IntFunction(name, tipe, args.map(_.tipe), isConstant, min, max, resourceType)
      }
      case parser.IntFunction(name, args, tipe, isConstant, min, max, None) => {
        new IntFunction(name, tipe, args.map(_.tipe), isConstant, min, max, "")
      }
      case parser.FloatFunction(name, args, tipe, isConstant, min, max, Some(resourceType)) => {
        new FloatFunction(name, tipe, args.map(_.tipe), isConstant, min, max, resourceType)
      }
      case parser.FloatFunction(name, args, tipe, isConstant, min, max, None) => {
        new FloatFunction(name, tipe, args.map(_.tipe), isConstant, min, max, "")
      }
    }
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
    addFunction(buildFunction(f).scoped(scope))
  }

  def addFunction(func : Function) {
    assert(!functions.contains(func), "Function "+func.name+" already exists.")
    functions(func.name) = func
  }

  /** Look up if there exists a function with this name.
    *
    * @param funcName Name of the function to look up.
    * @return True if such a function exists, False otherwise.
    */
  def isDefined(funcName:String) = functions.contains(funcName)

  /** Returns true if the given function name maps to a constant function
    *
    * @param funcName Name of the function to look up.
    * @return True if the function is constant, False otherwise.
    */
  def isConstantFunc(funcName:String) = functions.contains(funcName)

  /** Returns all functions stored in this function manager */
  def getAll : java.util.List[Function] = seqAsJavaList(functions.values.toList)

  /** Finds the definition of the function with the given name.
    *
    * @param functionName Name of the function to look up.
    * @return The function definition. Throws an [[planstack.anml.ANMLException]] if no such function can be found.
    */
  def get(functionName:String) = {
    if(functions.contains(functionName))
      functions(functionName)
    else
      throw new ANMLException("Unknown function name: "+functionName)
  }
}
