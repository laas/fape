package planstack.anml.model

import collection.JavaConversions._
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import scala.collection.mutable
import planstack.anml.{ANMLException, parser}
import planstack.anml.model.concrete.VarRef


class InstanceManager {

  private val typeHierarchy = new SimpleUnlabeledDirectedAdjacencyList[String]()
  private val types = mutable.Map[String, Type]()
  /**
   * Maps an instance name to a (type, GlobalReference) pair
   */
  private val instancesDef = mutable.Map[String, Pair[String, VarRef]]()
  private val instancesByType = mutable.Map[String, List[String]]()

  addType("boolean", "")
  addInstance("true", "boolean")
  addInstance("false", "boolean")
  addType("object", "")



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
  def containsInstance(instanceName:String) = instancesDef.contains(instanceName)

  def containsType(typeName:String) = types.contains(typeName)

  def typeOf(instanceName:String) = instancesDef(instanceName)

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

  def instancesOfType(tipe:String) : java.util.List[String] = seqAsJavaList(instancesOfTypeRec(tipe))

  /** Returns all instances of the given type */
  private def instancesOfTypeRec(tipe:String) : List[String] = {
    instancesByType(tipe) ++ typeHierarchy.children(tipe).map(instancesOfTypeRec(_)).flatten
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
