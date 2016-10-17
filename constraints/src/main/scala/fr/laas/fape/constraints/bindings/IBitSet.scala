package fr.laas.fape.constraints.bindings

object IBitSet {
  private final val WORD_MASK = 0xffffffffffffffffL
  private final val BITS_PER_WORD = 64

  private def updateArray(elems: Array[Long], idx: Int, w: Long): Array[Long] = {
    var len = elems.length
    while (len > 0 && (elems(len - 1) == 0L || w == 0L && idx == len - 1)) len -= 1
    var newlen = len
    if (idx >= newlen && w != 0L) newlen = idx + 1
    val newelems = new Array[Long](newlen)
    Array.copy(elems, 0, newelems, 0, len)
    if (idx < newlen) newelems(idx) = w
    else assert(w == 0L)
    newelems
  }
}

class IBitSet(val elems: Array[Long]) extends Set[Int] {
  def this() = this(Array.fill(1)(0))
  import IBitSet._
  val nwords = elems.length

  private def words(i: Int) : Long =
    if(i < elems.length) elems(i)
    else 0L
  private def wordIndex(i: Int) = i >> 6

  override def contains(elem: Int): Boolean =
    0 <= elem  && (words(elem >> 6) & (1L << elem)) != 0L

  override def +(elem: Int): IBitSet =
    if(contains(elem))
      this
    else {
      val w = words(elem >> 6) | (1L << elem)
      new IBitSet(updateArray(elems, elem >> 6, w))
    }

  override def -(elem: Int): IBitSet =
    if(!contains(elem))
      this
    else {
      val w = words(elem >> 6) & ~(1L << elem)
      new IBitSet(updateArray(elems, elem >> 6, w))
    }

  private def nextSetBit(from: Int): Int = {
    require(from >= 0)
    var u = wordIndex(from)
    if(u >= nwords)
      return -1
    var word = words(u) & (WORD_MASK << from)
    while(true) {
      if(word != 0) {
        val ret = (u * BITS_PER_WORD) + java.lang.Long.numberOfTrailingZeros(word)
        assert(contains(ret))
        return ret
      }
      u += 1
      if(u == nwords)
        return -1
      word = words(u)
    }
    throw new RuntimeException("Problem")
  }

  def &(o: IBitSet) : IBitSet = {
    val newSize = Math.min(nwords, o.nwords)
    val intersection = new Array[Long](newSize)
    var i = 0
    while(i < newSize) {
      intersection(i) = words(i) & o.words(i)
      i += 1
    }
    new IBitSet(intersection)
  }

  def |(o: IBitSet) : IBitSet = {
    val newSize = Math.max(nwords, o.nwords)
    val union = new Array[Long](newSize)
    var i = 0
    while(i < newSize) {
      union(i) = words(i) | o.words(i)
      i += 1
    }
    new IBitSet(union)
  }

  def sharesOneElement(o: IBitSet) : Boolean = {
    val newSize = Math.min(nwords, o.nwords)
    var i = 0
    while(i < newSize) {
      if(words(i) != 0 && o.words(i) != 0 && (words(i) & o.words(i)) != 0)
        return true
      i += 1
    }
    return false
  }

  override def iterator: Iterator[Int] = new Iterator[Int] {
    private var current = nextSetBit(0)
    override def hasNext: Boolean = current != -1
    override def next(): Int = {  val ret = current ; current = nextSetBit(current+1) ; ret }
  }

  override final lazy val isEmpty : Boolean = {
    var i = nwords
    var empty = true
    while(empty && i > 0) {
      i -= 1
      if(words(i) != 0)
        empty = false
    }
    empty
  }

  override final lazy val size: Int = {
    var s = 0
    var i = nwords
    while (i > 0) {
      i -= 1
      s += java.lang.Long.bitCount(words(i))
    }
    s
  }
}