package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.stn.variables.TemporalInterval
import fr.laas.fape.constraints.meta.variables.Variable
import org.scalatest.FunSuite

class JobShopTest extends FunSuite {

  val instance = new JobShopInstance(4, List(List(2, 4, 2, 1), List(5, 3, 2), List(3, 5, 7)), Some(14))
  // val instance = new JobShopInstance(2, List(List(2, 4))) // very simple instance to avoid taking time in unit tests

  test("job shop search") {
    val (model, jobs) = jobShopModel(instance)

    BinarySearch.count = 0
    implicit val csp = BinarySearch.search(model, optimizeMakespan = true)
    assert(csp != null)
    assert(csp.isSolution)
    assert(instance.optimalMakespan.isEmpty || csp.makespan == instance.optimalMakespan.get)

    // println(csp.report)
    for((m, js) <- jobs.groupBy(_.machine.domain.lb).toList.sortBy(_._1)) {
      print(s"$m: ")
      val localJobs = js.sortBy(_.interval.start.domain.lb)
      println(localJobs.map(j => s"${j.interval.start.domain.lb}[${j.duration}]:(${j.jobNumber}, ${j.numInJob})").mkString("  --  "))
    }
    println("Makespan: "+csp.temporalHorizon.domain.lb)
    println("Num nodes: " + BinarySearch.count)
    println("Num constraints: "+csp.constraints.satisfied.size)
  }

  def jobShopModel(instance: JobShopInstance) : (CSP, Seq[Job]) = {
    implicit val csp = new CSP
    val jobs =
      for(i <- instance.jobs.indices ; j <- instance.jobs(i).indices) yield {
        val int = new TemporalInterval(csp.varStore.getTimepoint(), csp.varStore.getTimepoint())
        val machine = csp.varStore.getVariable(Some(s"machine($i,$j)"))
        csp.addVariable(machine, (1 to instance.numMachines).toSet)

        new Job(i, j, instance.jobs(i)(j), int, machine)
      }

    // set temporal constraints
    for(i <- jobs.indices) {
      val job = jobs(i)
      csp.post(job.interval.duration == job.duration -1)
      if(job.numInJob >= 1)
        csp.post(jobs(i-1).interval.end < job.interval.start)
    }

    for(j1 <- jobs ; j2 <- jobs ; if j1 != j2) {
      csp.post(j1.machine =!= j2.machine || j1.interval < j2.interval || j1.interval > j2.interval)
    }
    (csp, jobs)
  }
}

case class Job(jobNumber: Int, numInJob: Int, duration: Int, interval: TemporalInterval, machine: Variable)

class JobShopInstance(val numMachines: Int, val jobs: Seq[Seq[Int]], val optimalMakespan: Option[Int]) {

  def numJobs = jobs.size
}
