package planstack.anml

import planstack.anml.model.AnmlProblem
import planstack.anml.parser.ANMLFactory

object Parsing extends App {

  val file =
    if(args.size == 0)
      "resources/test.anml"
    else
      args(0)
  println("Parsing: "+file)

  val pb = new AnmlProblem(usesActionConditions = true)
  pb.extendWithAnmlFile(file)

  val breakPoint = true
}
