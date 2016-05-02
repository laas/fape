package planstack.anml.model

import planstack.anml.ANMLException
import planstack.anml.model.concrete.{InstanceRef, RefCounter}
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList

import scala.collection.JavaConversions._
import scala.collection.mutable


class InstanceManager(val refCounter: RefCounter) {

  /** Root of the type Hierarchy */
  private val NON_NUM_SOURCE_TYPE = "__NON_NUM_SOURCE_TYPE__"

  /** Maps every type name to a full type definition */
  private val types = mutable.Map[String, SimpleType]()

  /** Maps an instance name to a (type, GlobalReference) pair */
  private val instancesDef = mutable.Map[String, Pair[String, InstanceRef]]()

  // predefined ANML types and instances
  addType(NON_NUM_SOURCE_TYPE,"")
  addType("boolean", "")
//  addType("integer", "")
  addInstance("true", "boolean", refCounter)
  addInstance("false", "boolean", refCounter)
  addType("typeOfUnknown", "")
  addInstance("unknown", "typeOfUnknown", refCounter)
  addType("_decompositionID_", "")
  for(i <- 0 until 20)
    addInstance("decnum:"+i, "_decompositionID_", refCounter)


  /** Creates a new instance of a certain type.
    *
    * @param name Name of the instance.
    * @param t Type of the instance.
    */
  def addInstance(name:String, t:String, refCounter: RefCounter) {
    assert(!instancesDef.contains(name), "Instance already declared: " + name)
    assert(types.contains(t), "Unknown type: " + t)
    val newInstance = new InstanceRef(name, t, refCounter)
    instancesDef(name) = (t, newInstance)
    types(t).addInstance(newInstance)
  }

  def addTypes(typeList: List[(String,String)]): Unit = {
    val q = mutable.Queue[(String,String)]()
    q.enqueue(typeList: _*)
    while(q.nonEmpty) {
      q.dequeue() match {
        case (typ,"") =>
          q.enqueue((typ, NON_NUM_SOURCE_TYPE))
        case t@(_,parent) if !types.contains(parent) =>
          q.enqueue(t)
        case (typ,parentName) =>
          addType(typ, parentName)
      }
    }
  }

  /** Records a new type.
    *
    * @param name Name of the type
    * @param parent Name of the parent type. If empty (""), no parent is set for this type.
    */
  def addType(name:String, parent:String) {
    assert(!types.contains(name), "Error: type \""+name+"\" is already recorded.")
    assert(parent.isEmpty || types.contains(parent), s"Parent type \'$parent\'  of \'$name\' is not defined yet.")

    types(name) = parent match {
      case "" => new SimpleType(name, None)
      case par => new SimpleType(name, Some(types(par)))
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
  def subTypes(typeName :String) : java.util.Set[String] = setAsJavaSet(types(typeName).allSubTypes.map(_.name))

  /** Returns all parents of this type */
  def parents(typeName: String) : java.util.Set[String] =  {
    setAsJavaSet(types(typeName).parents.map(_.toString))
  }

  /**
   * @return All instances as a list of (name, type) pairs
   */
  def instances : List[Pair[String, String]] = instancesDef.toList.map(x => (x._1, x._2._1))

  /** Return a collection containing all instances. */
  def allInstances : java.util.Collection[String] =  asJavaCollection(instancesDef.keys)

  /** Retrieves the variable reference linked to this instance
    *
    * @param name Name of the instance to lookup
    * @return The global variable reference linked to this instance
    */
  def referenceOf(name: String) : InstanceRef = instancesDef(name)._2

  def referenceOf(value: Int) : InstanceRef = {
    if (!instancesDef.contains(value.toString))
      addInstance(value.toString, "integer", refCounter)
    instancesDef(value.toString)._2
  }

  /** Lookup for all instances of this types (including all instances of any of its subtypes). */
  def instancesOfType(tipe:String) : java.util.List[String] = seqAsJavaList(instancesOfTypeRec(tipe))

  /** Returns all instances of the given type */
  private def instancesOfTypeRec(tipe:String) : List[String] = {
    assert(tipe != "integer", "Requested instances of type integer.")
    assert(types.contains(tipe), s"Unknown type: $tipe")
    types(tipe).instances.toList.map(_.instance)
  }

  /**
   * Checks if the type an accept the given value. This is true if
   *  - the value's type is subtype of typ
   *  - the value is "unknown" (always aceptable value)
    *
    * @param value Value to be checked.
   * @param typ Type that should accept the value.
   * @param context Context in which the value is declared (used to retrieve its type.
   * @return True if the value is acceptable.
   */
  def isValueAcceptableForType(value:LVarRef, typ:String, context: AbstractContext) : Boolean =
    subTypes(typ).contains(context.getType(value)) || value.id == "unknown"

  /** Returns all instances of the given type */
  def jInstancesOfType(tipe:String) = seqAsJavaList(instancesOfType(tipe))

  /** Return a fully qualified function definition in the form
    * [Robot, location].
    *
    * @param typeName base type in which the method is used
    * @param methodName name of the method
    * @return
    */
  def getQualifiedFunction(typeName:String, methodName:String) : List[String] = {
    assert(types.contains(typeName), s"Type $typeName does not seem to exist.")
    val ret = types(typeName).getQualidiedFunction(methodName)
    assert(ret.nonEmpty)
    ret.split("\\.").toList
  }
}
