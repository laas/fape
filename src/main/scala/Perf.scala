import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer


object Perf {

  def main(args: Array[String]) {
    def t = System.currentTimeMillis

    val n = 10000
    var treemap = new TreeMap[Int, Int]
    val array = new ArrayBuffer[Int](n)

    var s = t


    for(i <- 0 to n-1)
      treemap = treemap.+((i, i))

    var e = t
    println(e - s)

    s = t

    for(i <- 0 to n-1)
      array += i

    e = t
    println(e - s)

  }
}