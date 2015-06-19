package planstack.constraints.bindings

import java.util

import scala.collection.mutable
import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object BindingConstraintNetwork {
  type DomID = Int
}

class BindingConstraintNetwork[VarRef](toCopy: Option[BindingConstraintNetwork[VarRef]]) extends BindingCN[VarRef] {

  import BindingConstraintNetwork.DomID

  private val increment = 10



  var domIds : mutable.Map[VarRef, DomID] = null
  var types : mutable.Map[VarRef, String] = null
  var domains : mutable.Map[DomID, ValuesHolder] = null
  var vars : ListBuffer[ListBuffer[VarRef]] = null
  var different : Array[Array[Boolean]] = null
  var values : ListBuffer[String] = null
  var valuesIds : Map[String, Int] = null
  var intValues : ListBuffer[Int] = null
  var intValuesIds : Map[Int, Int] = null

  var defaultIntDomain : ListBuffer[ValuesHolder] = null

  /** Extension constraints */
  var extensionConstraints : Map[String, ExtensionConstraint] = null
  var mapping : ArrayBuffer[(mutable.Buffer[DomID], String)] = null


  var queue : ListBuffer[DomID] = null

  var unusedDomainIds : mutable.ListBuffer[DomID] = null

  var hasEmptyDomains = false

  var listener : IntBindingListener[VarRef] = null

  toCopy match {
    case Some(o) =>
      domIds = o.domIds.clone()
      types = o.types
      domains = o.domains.clone()
      vars = o.vars.map(x => x.clone())
      different = o.different.map(x => x.clone())
      values = o.values
      valuesIds = o.valuesIds
      intValues = o.intValues
      intValuesIds = o.intValuesIds
      defaultIntDomain = o.defaultIntDomain
      extensionConstraints = o.extensionConstraints
      mapping = o.mapping.clone()
      unusedDomainIds = o.unusedDomainIds.clone()
      queue = o.queue.clone()
    case None =>
      domIds = mutable.Map[VarRef, DomID]()
      types = mutable.Map[VarRef, String]()
      domains = mutable.Map[DomID, ValuesHolder]()
      vars = ListBuffer[ListBuffer[VarRef]]()
      different = new Array[Array[Boolean]](increment)
      for(i <- 0 until different.size)
        different(i) = Array.fill(increment)(false)

      values = ListBuffer[String]()
      valuesIds = Map[String, Int]()
      intValues = ListBuffer[Int]()
      intValuesIds = Map[Int, Int]()
      defaultIntDomain = ListBuffer(new ValuesHolder(Set()))

      extensionConstraints = Map()
      mapping = ArrayBuffer()

      unusedDomainIds = ListBuffer[DomID]()
      queue = ListBuffer[DomID]()
  }

  private def domID(v: VarRef) : DomID = domIds(v)

  private def allDomIds = (0 until vars.size).filterNot(unusedDomainIds.contains(_))

  private def newDomID() : DomID = {
    val id =
      if(unusedDomainIds.nonEmpty) {
        val next = unusedDomainIds.head
        unusedDomainIds -= next
        next
      } else {
        val next = vars.size
        next
      }

    if(vars.size > id) vars(id) = ListBuffer[VarRef]()
    else vars += ListBuffer[VarRef]()

    if(id < different.size) {
      // replace with bigger
      val newConstraints = new Array[Array[Boolean]](different.size + increment)
      for(i <- 0 until newConstraints.size)
        newConstraints(i) = Array.fill(different.size +increment)(false)

      for(i <- 0 until different.size)
        Array.copy(different(i), 0, newConstraints(i), 0, different(i).size)
    }

    id
  }


  override def rawDomain(v: VarRef) : ValuesHolder = domains(domID(v))

  def isDiff(v1: VarRef, v2: VarRef) = different(domID(v1))(domID(v2))

  private def domainChanged(id: DomID): Unit = {
    if(!queue.contains(id))
      queue += id

    if(listener != null && domains(id).size() == 1 && isIntegerVar(vars(id).head)) {
      val value = intValues(domains(id).head())
      for(v <- vars(id)) {
        assert(isIntegerVar(v))
        listener.onBinded(v, value)
      }
    }
  }


  override def setListener(listener: IntBindingListener[VarRef]) { this.listener = listener }

  override def domainOfIntVar(v: VarRef): util.List[Integer] = {
    assert(isIntegerVar(v))
    rawDomain(v).vals.map(intValues(_).asInstanceOf[Integer]).toList.asJava
  }

  override def isIntegerVar(v: VarRef): Boolean =
    typeOf(v) == "integer" || typeOf(v) == "int"

  override def stringValuesAsDomain(stringDomain: util.Collection[String]): ValuesHolder =
    new ValuesHolder(stringDomain.asScala.map(valuesIds(_)))

  override def typeOf(v: VarRef): String = types(v)

  override def unifiable(a: VarRef, b: VarRef): Boolean =
    !isDiff(a, b) && rawDomain(a).intersect(rawDomain(b)).nonEmpty

  override def domainOf(v: VarRef): util.List[String] =
    rawDomain(v).values().asScala.map(values(_)).toList.asJava

  override def unified(a: VarRef, b: VarRef): Boolean =
    domID(a) == domID(b) || domainSize(a) == 1 && domainSize(b) == 1 && rawDomain(a).head() == rawDomain(b).head()

  override def addValuesToValuesSet(setID: String, values: util.List[String]): Unit = {
    if(!extensionConstraints.contains(setID)) {
      extensionConstraints += ((setID, new ExtensionConstraint(false)))
    }
    val valuesAsIDs = values.asScala.map(valuesIds(_).asInstanceOf[Integer]).toList.asJava
    extensionConstraints(setID).addValues(valuesAsIDs)
  }

  override def addValuesToValuesSet(setID: String, values: util.List[String], lastVal: Int): Unit = {
    if(!extensionConstraints.contains(setID)) {
      extensionConstraints += ((setID, new ExtensionConstraint(true)))
    }
    val valuesAsIDs = values.asScala.map(valuesIds(_).asInstanceOf[Integer]).toList.asJava
    valuesAsIDs.add(intValuesIds(lastVal))
    extensionConstraints(setID).addValues(valuesAsIDs)
  }


  override def separated(a: VarRef, b: VarRef): Boolean =
    isDiff(a, b)

  override def separable(a: VarRef, b: VarRef): Boolean =
    !isDiff(a, b)

  override def addPossibleValue(value: String) {
    assert(!valuesIds.contains(value))
    valuesIds += ((value, values.size))
    values += value
  }

  override def addPossibleValue(value: Int): Unit = {
    if(!intValuesIds.contains(value)) {
      intValuesIds += ((value, intValues.size))
      intValues += value
      defaultIntDomain(0) = defaultIntDomain(0).add(value)
    }
  }

  override def addValuesSetConstraint(variables: util.List[VarRef], setID: String): Unit = {
    val asIDs : mutable.Buffer[DomID] = variables.asScala.map(domID(_))
    mapping += ((asIDs, setID))
  }

  override def restrictIntDomain(v: VarRef, toValues: util.Collection[Integer]): Unit =
    restrictDomain(v, intValuesAsDomain(toValues))

  override def AddSeparationConstraint(a: VarRef, b: VarRef): Unit = {
    if(domID(a) == domID(b))
      hasEmptyDomains = true

    different(domID(a))(domID(b)) = true
    different(domID(b))(domID(a)) = true

    queue += domID(a)
    queue += domID(b)
  }

  override def isConsistent: Boolean = {
    while(queue.nonEmpty) {
      val cur = queue.head
      queue -= cur
      if(domains(cur).size() == 0) {
        hasEmptyDomains = true
      } else if(domains(cur).size() == 1) {
        val uniqueValue = domains(cur).head()
        for (o <- 0 until vars.size; if !unusedDomainIds.contains(o); if different(cur)(o)) {
          if(domains(o).contains(uniqueValue)) {
            domains(o) = domains(o).remove(uniqueValue)

            if(domains(o).isEmpty)
              hasEmptyDomains = true

            domainChanged(o)
          }
        }
      }
    }

    !hasEmptyDomains
  }

  override def domainAsString(v: VarRef): String = "{"+ rawDomain(v).vals.map(values(_)).mkString(", ") +"}"

  override def Report(): String =
    allDomIds.map(id => (id, "["+ vars(id).mkString(", ") +"]", "  "+domainAsString(vars(id).head))).mkString("\n")

  private def merge(id1: DomID, id2: DomID) {
    val newDom = domains(id1).intersect(domains(id2))
    val domainUpdated = newDom.size() < domains(id1).size() || newDom.size() < domains(id2).size()

    domains(id1) = newDom
    vars(id1) = vars(id1) ++ vars(id2)
    for(i <- 0 until different(id1).size)
      different(id1)(i) = different(id1)(i) && different(id2)(i)
    for(v <- vars(id2))
      domIds(v) = id1
    if(queue.contains(id2))
      queue -= id2

    unusedDomainIds += id2
    vars(id2) = new ListBuffer[VarRef]()
    for(i <- 0 until different.size) {
      different(i)(id2) = false
      different(id2)(i) = false
    }
    domains -= id2

    for(i <- 0 until mapping.size) {
      if(mapping(i)._1.contains(id2)) {
        val newDef = mapping(i)._1.map(id => if(id == id1) id1 else id)
        mapping(i) = (newDef, mapping(i)._2)
      }
    }

    if(domainUpdated)
      domainChanged(id1)

    assert(allDomIds.forall(id => vars(id).nonEmpty))
  }

  override def AddUnificationConstraint(a: VarRef, b: VarRef): Unit = {
    if(domID(a) != domID(b)) {
      merge(domID(a), domID(b))
    }
  }

  override def restrictDomain(v: VarRef, domain: ValuesHolder): Boolean = {
    val newDom = rawDomain(v).intersect(domain)
    val modified = newDom.size() < rawDomain(v).size()
    if(modified) {
      domains(domID(v)) = newDom
      domainChanged(domID(v))
    }
    modified
  }

  override def restrictDomain(v: VarRef, toValues: util.Collection[String]): Unit =
    restrictDomain(v, stringValuesAsDomain(toValues))

  override def getUnboundVariables: util.List[VarRef] = {
    val unboundDomains = for ((domId, dom) <- domains; if dom.size() != 1) yield domId
    unboundDomains.map(vars(_).head).toList.asJava
  }

  override def AddVariable(v: VarRef, domain: util.Collection[String], typ: String): Unit = {
    addVariable(v, stringValuesAsDomain(domain), typ)
  }

  private def addVariable(v:VarRef, dom: ValuesHolder, typ: String) {
    assert(!contains(v))
    val domID = newDomID()
    types(v) = typ
    domIds(v) = domID
    vars(domID) += v
    domains(domID) = dom
    domainChanged(domID)

    assert(vars(domID).size == 1)
  }

  override def contains(v: VarRef): Boolean = domIds.contains(v)

  override def DeepCopy(): BindingConstraintNetwork[VarRef] =
    new BindingConstraintNetwork[VarRef](Some(this))

  override def assertGroundAndConsistent(): Unit = ???

  override def domainSize(v: VarRef): Integer = rawDomain(v).size()

  override def AddIntVariable(v: VarRef): Unit =
    addVariable(v, defaultIntDomain.head, "integer")

  override def AddIntVariable(v: VarRef, domain: util.Collection[Integer]): Unit =
    addVariable(v, intValuesAsDomain(domain), "integer")

  override def intValuesAsDomain(intDomain: util.Collection[Integer]): ValuesHolder = {
    val valuesIds = intDomain.asScala.map(value => intValuesIds(value))
    new ValuesHolder(valuesIds)
  }

  override def intValueOfRawID(valueID: Integer): Integer = intValues(valueID)
}
