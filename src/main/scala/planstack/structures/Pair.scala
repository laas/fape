package planstack.structures

class Pair[T,V](val v1:T, val v2:V) {

  override def toString = "(%s, %s)".format(v1, v2)
}
