package fr.laas.fape.anml

import fr.laas.fape.anml.model.AnmlProblem

object Parsing extends App {

  val repetitions = 10

  val file =
    if(args.size == 0)
      "resources/test.anml"
    else
      args(0)
  println("Parsing: "+file)

  for(i <- 0 until repetitions) {

    val start = System.currentTimeMillis()

    val pb = new AnmlProblem()
    val parsed = System.currentTimeMillis()
    pb.extendWithAnmlFile(file)
    val extended = System.currentTimeMillis()

    println(s"Time parsing: ${parsed - start}")
    println(s"Time extending: ${extended - parsed}")
  }
}
