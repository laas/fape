package planstack.constraints.stn

import planstack.graph.printers.GraphDotPrinter

class JobShopProblem(val numJobs:Int, val numTasks:Int, val deadline:Int) {

  val jobs = Array.ofDim[Int](numJobs, numTasks)

  val startMap = Array.ofDim[Int](numJobs, numTasks)
  val endMap = Array.ofDim[Int](numJobs, numTasks)

  def s(t:JobShopTask) = startMap(t.jid)(t.tid)
  def e(t:JobShopTask) = endMap(t.jid)(t.tid)

  val stn = new STNIncBellmanFord

  stn.addConstraint(stn.start, stn.end, deadline)

  def addTask(t:JobShopTask) {
    jobs(t.jid)(t.tid) = t.d

    startMap(t.jid)(t.tid) = stn.addVar()
    endMap(t.jid)(t.tid) = stn.addVar()


    stn.addConstraint(s(t), e(t), t.d)
    stn.addConstraint(e(t), s(t), -t.d)

    if(t.tid > 0)
      stn.addConstraint(s(t), endMap(t.jid)(t.tid-1), 0)



    if(!stn.consistent) {
      val s = stn.asInstanceOf[STNIncBellmanFord]
      for(e <- s.g.edges()) {
        println("%d -> %d : %d".format(e.u, e.v, e.l))
      }

      var str = "IBF : " + s.consistent.toString + " -> "
      for(i <- 0 to s.forwardDist.length-1) {
        str += "(%d %s)".format(i, s.forwardDist(i))
      }
      str += "\n           <- "
      for(i <- 0 to s.backwardDist.length-1) {
        str += "(%d %s)".format(i, s.backwardDist(i))
      }
      println(str)

      new GraphDotPrinter(stn.g).print2Dot("/home/abitmonn/these/Documents/Experiments/tmp/g.dot")

      throw new Exception("Problem")
    }
    
  }
}
