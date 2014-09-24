package planstack.structures

class Pair[T,V](val value1:T, val value2:V) {

  override def toString = "(%s, %s)".format(value1, value2)
}
