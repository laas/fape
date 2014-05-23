package planstack.anml

import planstack.anml.parser.ANMLFactory

object Parsing extends App {

  if(args.size == 0)
    println("Error: give one anml file to parse.")
  else {
    val file = args(0)
    println("Parsing: "+file)

    println(ANMLFactory.parseAnmlFromFile(file))
  }


}
