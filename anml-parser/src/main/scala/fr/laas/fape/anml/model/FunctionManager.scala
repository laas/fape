package fr.laas.fape.anml.model

import fr.laas.fape.anml
import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.parser.PType
import fr.laas.fape.anml.parser

import scala.collection.JavaConversions._
import scala.collection.mutable

/** Representation of an ANML function.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  */
abstract class Function(val name:String, val valueType:Type, val argTypes:List[Type], val isConstant:Boolean) {

  override def toString =
    name+"("+argTypes.mkString(",")+"):"+valueType

  /** Builds a version of this defined as if it was scoped inside this containingType.
    * if the function is `AType aFunction(Object)`, invoking `scoped("Container)` on it would result in:
    * `AType Container.aFunction(Container, Object)`
    *
    * @param containingType Type in which the function was declared.
    * @return THe scoped version of the function.
    */
  def scoped(containingType : Type) : Function
}

/** Representation of an ANML symbolic function.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param valueType Type of the function's value.
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  */
class SymFunction(name:String, valueType:Type, argTypes:List[Type], isConstant:Boolean)
  extends Function(name, valueType, argTypes, isConstant)
{
  def scoped(containingType : Type) : SymFunction =
    new SymFunction(containingType+"."+name, valueType, containingType::argTypes, isConstant)
}

/** Representation of an ANML numeric function (on float or integer).
  * See derived classes [[fr.laas.fape.anml.model.IntFunction]] and [[fr.laas.fape.anml.model.FloatFunction]]
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
abstract class NumFunction(name:String, valueType:NumericType, argTypes:List[Type], isConstant:Boolean, val resourceType:String)
  extends Function(name, valueType, argTypes, isConstant)
{
  /** Returns true if this function has a specific resource type in consumable, replenishable, reusable or producible.
    * If this is the case, you should check `resourceType` to see what it is.
    */
  def hasSpecificResourceType : Boolean = resourceType != ""
}

/** Representation of an ANML function on integers.
  *
  * @param name Name of the function. the function name can be of the form `functionName` if it is defined at the root of
  *             an anml problem or `typeName.scopedFunctionName` if it is defined in a type
  * @param argTypes Types of the arguments of the function in order.
  * @param isConstant True if this function is defined as constant. False otherwise
  * @param minValue Minimum possible value of the function. It is set to `Integer.MIN_VALUE` if no lower bound is
  *                 specified in the ANML model.
  * @param maxValue Maximum possible value of the function. It is set to `Integer.MAX_VALUE` if no upper bound is
  *                 specified in the ANML model.
  * @param resourceType Type of the resource which can be one of consumable, replenishable, reusable or producible.
  *                     If it is an empty String, then it is a generic resource.
  */
class IntFunction(name:String,  argTypes:List[Type], isConstant:Boolean, val minValue:Int, val maxValue:Int, resourceType:String)
  extends NumFunction(name, TInteger, argTypes, isConstant, resourceType)
{
  require(maxValue > Int.MinValue)

  def scoped(container : Type) : IntFunction =
    new IntFunction(container+"."+name, container::argTypes, isConstant, minValue, maxValue, resourceType)
}

/** Storage for functions found in an [[fr.laas.fape.anml.model.AnmlProblem]] */
class FunctionManager(val pb:AnmlProblem) {

  /**
   * Maps function name to definition.
   * Those function have an implicit time parameter which is dealt with externally
   */
  private val functions = mutable.Map[String, Function]()

  /** Converts a function from the parser model and adds it to this function manager. */
  def addFunction(f:anml.parser.Function) {
    addFunction(buildFunction(f))
  }

  /** Builds a Function from the output of the ANML parser. */
  private def buildFunction(f : anml.parser.Function) : Function = {
    def t(typeName: PType) = pb.instances.asType(typeName)
    f match {
      case parser.SymFunction(name, args, tipe, isConstant) =>
        new SymFunction(name, t(tipe), args.map(a => t(a.tipe)), isConstant)
      case parser.IntFunction(name, args, tipe, isConstant, min, max, Some(resourceType)) => {
        new IntFunction(name, args.map(a => t(a.tipe)), isConstant, min, max, resourceType)
      }
      case parser.IntFunction(name, args, tipe, isConstant, min, max, None) => {
        new IntFunction(name, args.map(a => t(a.tipe)), isConstant, min, max, "")
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
  def addScopedFunction(scope:Type, f:anml.parser.Function) {
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
    * @return The function definition. Throws an [[ANMLException]] if no such function can be found.
    */
  def get(functionName:String) : Function = {
    if(functions.contains(functionName))
      functions(functionName)
    else
      throw new ANMLException("Unknown function name: "+functionName)
  }
}
