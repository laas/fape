import planstack.constraints.stn.{Weight, STNIBF}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.Predef._



object App {

  def newEdge(numVert:Int) : Tuple3[Int, Int, Int] = {

    return (1, 2 ,3)
  }

  def main(args: Array[String]) {
    println("Hello,  world!")
    val str = "coucojdskldsf"

    val a = new Weight()
    val b = new Weight(2)
    val c = new Weight(5)

    println(a>b)
    println(a>c)
    println(b>a)
    println(c>a)
    println(b>c)
    println(c>b)

    println(a+2)
    println(a+b)
    println(b+c)
    println(b+2)
    println( b + 2 < c)

    val stn = new STNIBF

    for(i <- 0 to 4) stn.addVar()

    stn.addConstraint(0, 1, 20)
    stn.addConstraint(1, 0, -10)
    stn.addConstraint(1, 2, 40)
    stn.addConstraint(2, 1, -30)
    stn.addConstraint(3, 2, 20)
    stn.addConstraint(2, 3, 0)
    stn.addConstraint(3, 4, 50)
    stn.addConstraint(4, 3, -40)
    stn.addConstraint(0, 4, 70)
    stn.addConstraint(4, 0, -50)
    stn.addConstraint(1, 0, -30)
    stn.g.outEdges(0).foreach( println _ )

    stn.g.inEdges(0).foreach( println _ )
  }

}

