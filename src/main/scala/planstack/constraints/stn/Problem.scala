package planstack.constraints.stn

class Problem(val numJobs:Int, val numTasks:Int, val deadline:Int) {

  val jobs = Array.ofDim[Int](numJobs, numTasks)

  val startMap = Array.ofDim[Int](numJobs, numTasks)
  val endMap = Array.ofDim[Int](numJobs, numTasks)

  def s(t:Task) = startMap(t.jid)(t.tid)
  def e(t:Task) = endMap(t.jid)(t.tid)

  val stn = new STNIBF
  val startVar = stn.addVar()
  val endVar = stn.addVar()

  stn.addConstraint(startVar, endVar, deadline)

  def addTask(t:Task) {
    jobs(t.jid)(t.tid) = t.d

    startMap(t.jid)(t.tid) = stn.addVar()
    endMap(t.jid)(t.tid) = stn.addVar()

    stn.addConstraint(s(t), startVar, 0)
    stn.addConstraint(endVar, e(t), 0)
    stn.addConstraint(s(t), e(t), t.d)
    stn.addConstraint(e(t), s(t), -t.d)

    if(t.tid > 0)
      stn.addConstraint(s(t), endMap(t.jid)(t.tid-1), 0)
  }
}
