package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.stn.variables.TemporalInterval
import fr.laas.fape.constraints.meta.variables.Variable
import org.scalatest.FunSuite

import scala.collection.mutable

class JobShopTest extends FunSuite {

  val instance = new JobShopInstance(2, List(List(2, 4), List(5, 3)))

  test("job shop search") {
    val (model, jobs) = jobShopModel(instance)

    implicit val csp = BinarySearch.search(model)
    assert(csp != null)
    println(csp.report)
    for((m, js) <- jobs.groupBy(_.machine.value).toList.sortBy(_._1)) {
      print(s"$m: ")
      println(js.map(j => s"${j.interval.start.domain.lb}:(${j.jobNumber}, ${j.numInJob})").mkString("  --  "))
      println("Makesplan: "+csp.temporalHorizon.domain.lb)
    }
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
      csp.post(job.interval.duration == job.duration)
      if(job.numInJob >= 1)
        csp.post(jobs(i-1).interval <= job.interval)
    }

    for(j1 <- jobs ; j2 <- jobs ; if j1 != j2) {
      csp.post(j1.machine =!= j2.machine || j1.interval < j2.interval || j1.interval >= j2.interval)
    }
    (csp, jobs)
  }
}

case class Job(jobNumber: Int, numInJob: Int, duration: Int, interval: TemporalInterval, machine: Variable)

class JobShopInstance(val numMachines: Int, val jobs: Seq[Seq[Int]]) {

  def numJobs = jobs.size
}
