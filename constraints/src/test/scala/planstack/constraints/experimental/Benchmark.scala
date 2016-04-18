package planstack.constraints.experimental

object Benchmark extends App {

  val folder = "/tmp/postnu"
  val pbFile = folder+"/"+"postnu-00111.tn"


  val tn = TemporalNetwork.loadFromFile(pbFile)
  println(tn)
}
