package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.DisjunctiveConstraint
import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.stn.constraint.MinDelayConstraint
import fr.laas.fape.constraints.meta.stn.variables.TemporalInterval
import fr.laas.fape.constraints.meta.types.{BaseType, TypedVariable}
import fr.laas.fape.constraints.meta.variables.IntVariable
import org.scalatest.FunSuite

class JobShopWithTypesTest extends FunSuite {

   val instance = new JobShopInstance(4, List(List(2, 4, 2, 1), List(5, 3, 2), List(3, 5, 7)), Some(14))
//  val instance = new JobShopInstance(2, List(List(2, 4), List(4, 3, 3)), None) // very simple instance to avoid taking time in unit tests

  test("job shop search with types") {
    val (model, jobs) = jobShopModel(instance)
    ???
    BinarySearch.count = 0
    implicit var csp = BinarySearch.search(model, optimizeMakespan = true)
    assert(csp != null)
    assert(csp.isSolution)
    assert(instance.optimalMakespan.isEmpty || csp.makespan == instance.optimalMakespan.get)

    println(csp.log.history)
    // println(csp.report)
    for((m, js) <- jobs.groupBy(_.machine.dom.head).toList.sortBy(_._1)) {
      print(s"$m: ")
      val localJobs = js.sortBy(_.interval.start.domain.lb)
      println(localJobs.map(j => s"${j.interval.start.domain.lb}[${j.duration}]:(${j.jobNumber}, ${j.numInJob})").mkString("  --  "))
    }
    println("Makespan: "+csp.temporalHorizon.domain.lb)
    println("Num nodes: " + BinarySearch.count)
    println("Num constraints: "+csp.constraints.satisfied.size)
  }

  def jobShopModel(instance: JobShopInstance) : (CSP, Seq[JobWithType]) = {
    val t = BaseType("Machine", (1 to instance.numMachines).map("machine "+_))
    implicit val csp = new CSP
    val jobs =
      for(i <- instance.jobs.indices ; j <- instance.jobs(i).indices) yield {
        val int = new TemporalInterval(csp.varStore.getTimepoint(), csp.varStore.getTimepoint())
        val machine =  new TypedVariable(s"machine($i,$j)", t)

        new JobWithType(i, j, instance.jobs(i)(j), int, machine)
      }

    // set temporal constraints
    for(i <- jobs.indices) {
      val job = jobs(i)
      csp.post(job.interval.duration === job.duration -1)
      if(job.numInJob >= 1)
        csp.post(new Precedes(jobs(i-1), jobs(i)))
    }

    for(j1 <- jobs ; j2 <- jobs ; if j1 != j2) {
      csp.post(new Threat(j1, j2))
    }
    (csp, jobs)
  }

  class Threat(j1: JobWithType, j2: JobWithType) extends
    DisjunctiveConstraint(List(j1.machine =!= j2.machine, j1.interval < j2.interval, j1.interval > j2.interval))
  {
    require(j1 != j2)
    override def toString = s"threat($j1, $j2)"
  }

  class Precedes(j1: JobWithType, j2: JobWithType) extends MinDelayConstraint(j1.interval.end, j2.interval.start, 1) {
    require(j1 != j2)
    override def toString = s"precedes($j1, $j2)"
  }

}


case class JobWithType(jobNumber: Int, numInJob: Int, duration: Int, interval: TemporalInterval, machine: TypedVariable[String]) {
  override def toString = s"j($jobNumber, $numInJob)"
}


