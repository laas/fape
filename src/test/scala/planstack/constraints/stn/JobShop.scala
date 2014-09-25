package planstack.constraints.stn

import scala.util.Random






object JobShop {

  def mem2String = {
    val mb = 1024*1024;

    //Getting the runtime reference from system
    val runtime = Runtime.getRuntime();

    //Print used memory
    "Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb
  }

  def test(size:Int) {
    val rand = new Random(System.currentTimeMillis())

    val pb = new JobShopProblem(size, size, 500000)
    for(i <- 0 to size -1) {
      for(j <- 0 to size -1 ) {
        pb.addTask(new JobShopTask(i, j, rand.nextInt(200)))
      }
      println(mem2String)
    }

  }

  def main(args: Array[String]) {
    println("JobShop")


    /*
    val filename = "res/instances/la40"
    val sc = new java.util.Scanner(new File(filename))
    sc.nextLine(); sc.nextLine(); sc.nextLine(); sc.nextLine();
    val lines = sc.nextInt()
    val columns = sc.nextInt()



    val pb = new planstack.constraints.stn.Problem(lines, columns, 5000)
    for(i <- 0 to lines-1) {
      for(j <- 0 to columns-1) {
        val m = sc.nextInt()
        val dur = sc.nextInt()
        allstn = pb.stn.clone() :: allstn
        pb.addTask(new planstack.constraints.stn.Task(i, j, dur))
      }
    }
    */

    println(mem2String)
    def t = System.currentTimeMillis
    val s = t
    test(20)
    println("Runtime: %d".format(t - s))

//    val stn = new planstack.constraints.stn.planstack.constraints.stn.STNIBF
//    stn.addVar(); stn.addVar(); stn.addVar(); stn.addVar();
//    stn.addConstraint(1, 2, 10)
//    stn.addConstraint(2, 0, 1)
//    stn.addConstraint(0, 3, 2)
//    stn.addConstraint(3, 2, -4)
//    stn.addConstraint(3, 1, 10)

    println("Runtime: %d".format(t - s))

    val runtime = Runtime.getRuntime()
    System.out.println(mem2String)
  }


}