package planstack.anml.model

import collection.JavaConversions._
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import scala.collection.mutable
import planstack.anml.{ANMLException, parser}
import planstack.anml.model.concrete.VarRef


class InstanceManager {

  private val typeHierarchy = new SimpleUnlabeledDirectedAdjacencyList[String]()

  /** Maps every type name to a full type definition */
  private val types = mutable.Map[String, Type]()

  /** Maps an instance name to a (type, GlobalReference) pair */
  private val instancesDef = mutable.Map[String, Pair[String, VarRef]]()

  /** Maps every type to a list of instance that are exactly of this type (doesn't contain instances of sub types */
  private val instancesByType = mutable.Map[String, List[String]]()

  // predefined ANML types and instances
  addType("boolean", "")
  addInstance("true", "boolean")
  addInstance("false", "boolean")
  addType("object", "")


  /** Creates a new instance of a certain type.
    *
    * @param name Name of the instance.
    * @param t Type of the instance.
    */
  def addInstance(name:String, t:String) {
    assert(!instancesDef.contains(name), "Instance already declared: " + name)
    assert(types.contains(t), "Unknown type: " + t)

    instancesDef(name) = (t, new VarRef())
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

  /** Adds a new scoped method to a type.
    *
    * @param typeName Type to whom the method belong
    * @param methodName Name of the method
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
  def containsInstance(instanceName:String) = instancesDef.contains(instanceName)

  /** Returns true if the type with the given name exists */
  def containsType(typeName:String) = types.contains(typeName)

  /** Returns the type of a given instance */
  def typeOf(instanceName:String) = instancesDef(instanceName)._1

  /** Returns all (including indirect) subtypes of the given parameter, including itself.
    *
    * @param typeName Name of the type to inspect.
    * @return All subtypes including itself.
    */
  def subTypes(typeName :String) : java.util.Set[String] = setAsJavaSet(subTypesRec(typeName))

  private def subTypesRec(typeName:String) : Set[String] = typeHierarchy.children(typeName).map(subTypes(_)).flatten + typeName

  /**
   * @return All instances as a list of (name, type) pairs
   */
  def instances : List[Pair[String, String]] = instancesDef.toList.map(x => (x._1, x._2._1))

  /** Return a collection containing all instances. */
  def allInstances : java.util.Collection[String] =  asJavaCollection(instancesDef.keys)

  /** Retrieves the variable reference linked to this instance
    * @param name Name of the instance to lookup
    * @return The global variable reference linked to this instance
    */
  def referenceOf(name: String) : VarRef = instancesDef(name)._2

  /** Lookup for all instances of this types (including all instances of any of its subtypes). */
  def instancesOfType(tipe:String) : java.util.List[String] = seqAsJavaList(instancesOfTypeRec(tipe))

  /** Returns all instances of the given type */
  private def instancesOfTypeRec(tipe:String) : List[String] = {
    instancesByType(tipe) ++ typeHierarchy.children(tipe).map(instancesOfTypeRec(_)).flatten
  }

  /** Returns all instances of the given type */
  def jInstancesOfType(tipe:String) = seqAsJavaList(instancesOfType(tipe))

  /** Return a fully qualified function definition in the form
    * [Robot, location].
    * @param typeName base type in which the method is used
    * @param methodName name of the method
    * @return
    */
  def getQualifiedFunction(typeName:String, methodName:String) : List[String] = {
    assert(types.contains(typeName))
    if(types(typeName).methods.contains(methodName)) {
      typeName :: methodName :: Nil
    } else if(types(typeName).parent.nonEmpty) {
      getQualifiedFunction(types(typeName).parent, methodName)
    } else {
      throw new ANMLException("Unable to find a method %s for type %s.".format(methodName, typeName))
    }
  }
}
