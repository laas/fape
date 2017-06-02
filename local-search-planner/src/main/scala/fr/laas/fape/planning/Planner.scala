package fr.laas.fape.planning

import java.io.File

import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.constraints.meta.search.TreeSearch
import fr.laas.fape.constraints.meta.{CSP, Configuration}
import fr.laas.fape.planning.events.{InitPlanner, PlanningHandler}

case class Config(file: File = new File("."))

object Planner extends App {

  val parser = new scopt.OptionParser[Config]("lcp") {
    head("lcp")

    arg[File]("anml-problem-file")
      .action((x, c) => c.copy(file = x))
      .text("ANML problem file for this planner to solve.")

    help("help").text("prints this usage text")
  }

  val conf: Config = parser.parse(args, Config()) match {
    case Some(x) => x
    case None => System.exit(1); null
  }

  val pb = Utils.problem(conf.file)
  val csp = Utils.csp(pb)
  val searcher = new TreeSearch(List(csp))
  searcher.incrementalDeepeningSearch() match {
    case Left(solution) =>
      println(solution.getHandler(classOf[PlanningHandler]).report)
    case _ =>
      println("No solution found.")
  }

}

object Utils {

  def problem(anmlProblemFile: File): AnmlProblem = {
    val pb = new AnmlProblem
    pb.extendWithAnmlFile(anmlProblemFile.getAbsolutePath)
    pb
  }

  def csp(pb: AnmlProblem) : CSP = {
    val csp = new CSP(Left(new Configuration(enforceTpAfterStart = false)))
    csp.addHandler(new PlanningHandler(csp, Left(pb)))
    csp.addEvent(InitPlanner)
    csp
  }
}
