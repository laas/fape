package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints._
import fr.laas.fape.constraints.meta.domains.Domain

object IntVariable {
  var intVariableCounter = 0
  def next() = { intVariableCounter += 1; intVariableCounter -1}
}

trait IVar {

  def unaryConstraints : Seq[Constraint] = Nil

  // constraints shorthand
  def ===(other: IVar) : EqualityConstraint = (this, other) match {
    case (v1: IntVariable, v2: IntVariable) => new VariableEqualityConstraint(v1, v2)
    case (v1: VariableSeq, v2: VariableSeq) => new VariableSeqEqualityConstraint(v1, v2)
    case _ => throw new RuntimeException(s"Unknown instantiation of equality constraints for variables: $this and $other")
  }


  def =!=(other: IVar) : InequalityConstraint = (this, other) match {
    case (v1: IntVariable, v2: IntVariable) => new VariableInequalityConstraint(v1, v2)
    case (v1: VariableSeq, v2: VariableSeq) => new VariableSeqInequalityConstraint(v1, v2)
    case _ => throw new RuntimeException(s"Unknown instantiation of equality constraints for variables: $this and $other")
  }
}

/** Denotes a variable whose domain can be retrieved and represented explicitly */
trait VarWithDomain extends IVar {
  def domain(implicit csp: CSP) : Domain

  def isBound(implicit csp: CSP) = domain.isSingleton

  def value(implicit csp: CSP) = {
    require(domain.isSingleton, "Can only request the value of a bound variable")
    domain.values.head
  }

  def ===(value: Int) : Constraint

  def =!=(value: Int) : Constraint
}

class IntVariable(val initialDomain: Domain, val ref: Option[Any]) extends VarWithDomain {
  private val id = IntVariable.next()

  def this(initialDomain: Domain) = this(initialDomain, None)

  def domain(implicit csp: CSP) = csp.dom(this)

  override def ===(value: Int) : BindConstraint = new BindConstraint(this, value)

  override def =!=(value: Int) : NegBindConstraint = new NegBindConstraint(this, value)

  override def toString = ref match {
    case Some(x) => s"$x"
    case None => s"v$id"
  }
}

class BooleanVariable(initialDomain: Domain, ref: Option[Any]) extends IntVariable(initialDomain, ref) {

  def this(initialDomain: Domain) = this(initialDomain, None)

  def isTrue(implicit csp: CSP): Boolean =
    domain.isSingleton && domain.contains(1)

  def isFalse(implicit csp: CSP): Boolean =
    domain.isSingleton && domain.contains(0)
}

class VariableSeq(val variables: Seq[IVar], ref: Option[Any]) extends IVar {

  def this(variables: Seq[IVar]) = this(variables, None)

  override def toString = ref match {
    case Some(x) => s"$x"
    case None => s"(${variables.map(_.toString).mkString(", ")})"
  }

}
