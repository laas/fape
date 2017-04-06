package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints._
import fr.laas.fape.constraints.meta.domains.Domain

trait IVar {

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
}

/** Denotes a variable whose domain can be retrieved and represented explicitly */
trait VarWithDomain extends IVar {
  def domain(implicit csp: CSP) : Domain

  def isBound(implicit csp: CSP) = domain.isSingleton

  def value(implicit csp: CSP) = {
    require(domain.isSingleton, "Can only request the value of a bound variable")
    domain.values.head
  }

  def ===(value: Int) : BindConstraint = new BindConstraint(this, value)

  def =!=(value: Int) : NegBindConstraint = new NegBindConstraint(this, value)

}

class Variable(val id: Int, ref: Option[Any]) extends VarWithDomain {

  def this(id: Int) = this(id, None)

  def domain(implicit csp: CSP) = csp.dom(this)

  override def toString = ref match {
    case Some(x) => s"$x"
    case None => s"v$id"
  }


  // equals and hash code
  override final val hashCode = id

  override def equals(o:Any) =
    o match {
      case v: Variable => this.id == v.id
      case _ => false
    }
}

class BooleanVariable(id: Int, ref: Option[Any]) extends Variable(id, ref) {

  def this(id: Int) = this(id, None)

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
