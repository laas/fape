package planstack.anml.model

import collection.JavaConversions._
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import scala.collection.mutable
import planstack.anml.{ANMLException, parser}


class InstanceManager {

  private val typeHierarchy = new SimpleUnlabeledDirectedAdjacencyList[String]()
  private val types = mutable.Map[String, Type]()
  private val instancesTypes = mutable.Map[String, String]()
  private val instancesByType = mutable.Map[String, List[String]]()

  addType("boolean", "")
  addInstance("true", "boolean")
  addInstance("false", "boolean")
  addType("object", "")



  def addInstance(name:String, t:String) {
    assert(!instancesTypes.contains(name), "Instance already declared: " + name)
    assert(types.contains(t), "Unknown type: " + t)

    instancesTypes(name) = t
    instancesByType(t) = name :: instancesByType(t)
  }

  /** Records a new type.
    *
    * @param name Name of the type
    * @param parent Name of the parent type. If empty (""), no parent is set for this type.
    */
  def addType(name:String, parent:String) {
    assert(!types.contains(name))

    types(name) = new Type(name, parent)
    typeHierarchy.addVertex(name)
    instancesByType(name) = Nil

    if(parent.nonEmpty) {
      assert(types.contains(parent), "Unknown parent type %s for type %s. Did you declare them in order?".format(parent, name))

      typeHierarchy.addEdge(parent, name)
    }
  }

  /**
   *
   * @param typeName Type to whom the method belong
   * @param methodName name of the method
   */
  def addMethodToType(typeName:String, methodName:String) {
    assert(types.contains(typeName))
    types(typeName).addMethod(methodName)
  }

  /**
   *
   * @param instanceName Name of the instance to lookup
   * @return True if an instance of name `instanceName` is known
   */
  def containsInstance(instanceName:String) = instancesTypes.contains(instanceName)

  def containsType(typeName:String) = types.contains(typeName)

  def typeOf(instanceName:String) = instancesTypes(instanceName)

  /**
   * @return All instances as a list of (name, type) pairs
   */
  def instances : List[Pair[String, String]]= instancesTypes.toList

  /** Returns all instances of the given type */
  def instancesOfType(tipe:String) : List[String] = {
    instancesByType(tipe) ++ typeHierarchy.children(tipe).map(instancesOfType(_)).flatten
  }

  /** Returns all instances of the given type */
  def jInstancesOfType(tipe:String) = seqAsJavaList(instancesOfType(tipe))

  /**
   * Return a fully qualified function definition in the form
   * [Robot, location].
   * @param typeName base type in which the method is used
   * @param methodName name of the method
   * @return
   */
  def getQualifiedFunction(typeName:String, methodName:String) : List[String] = {
    assert(types.contains(typeName))
    if(types(typeName).methods.contains(methodName)) {
      typeName :: methodName :: Nil
    } else if(types(typeName).parent nonEmpty) {
      getQualifiedFunction(types(typeName).parent, methodName)
    } else {
      throw new ANMLException("Unable to find a method %s for type %s.".format(methodName, typeName))
    }
  }
}
