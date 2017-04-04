package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints._
import fr.laas.fape.constraints.meta.domains.Domain

abstract class IVar(val id: Int) {

  // constraints shorthand

  def ===(other: IVar) : EqualityConstraint = (this, other) match {
    case (v1: Variable, v2: Variable) => new VariableEqualityConstraint(v1, v2)
    case (v1: VariableSeq, v2: VariableSeq) => new VariableSeqEqualityConstraint(v1, v2)
    case _ => throw new RuntimeException(s"Unknown instantiation of equality constraints for variables: $this and $other")
  }


  def =!=(other: IVar) : InequalityConstraint = (this, other) match {
    case (v1: Variable, v2: Variable) => new VariableInequalityConstraint(v1, v2)
    case (v1: VariableSeq, v2: VariableSeq) => new VariableSeqInequalityConstraint(v1, v2)
    case _ => throw new RuntimeException(s"Unknown instantiation of equality constraints for variables: $this and $other")
  }

  // equals and hash code

  override final val hashCode = id

  override def equals(o:Any) =
    o match {
      case v: IVar => this.id == v.id
      case _ => false
    }
}

trait WithDomain {
  def domain(implicit csp: CSP) : Domain
}

class Variable(id: Int, ref: Option[Any]) extends IVar(id) with WithDomain {

  def this(id: Int) = this(id, None)

  def domain(implicit csp: CSP) = csp.dom(this)

  def isBound(implicit csp: CSP) = domain.isSingleton

  def value(implicit csp: CSP) = {
    require(domain.isSingleton, "Can only request the value of a bound variable")
    domain.values.head
  }

  override def toString = ref match {
    case Some(x) => s"$x"
    case None => s"v$id"
  }
}

class BooleanVariable(id: Int, ref: Option[Any]) extends Variable(id, ref) {

  def this(id: Int) = this(id, None)

  def isTrue(implicit csp: CSP): Boolean =
    domain.isSingleton && domain.contains(1)

  def isFalse(implicit csp: CSP): Boolean =
    domain.isSingleton && domain.contains(0)
}

class VariableSeq(val variables: Seq[IVar], id: Int, ref: Option[Any]) extends IVar(id) {

  def this(variables: Seq[IVar], id: Int) = this(variables, id, None)

  override def toString = ref match {
    case Some(x) => s"$x"
    case None => s"(${variables.map(_.toString).mkString(", ")})"
  }

}
