package planstack.constraints.stn

import scala.util.Random






object Main {

  def test(size:Int) {
    val rand = new Random(System.currentTimeMillis())

    val pb = new Problem(size, size, 500000)
    for(i <- 0 to size -1) {
      for(j <- 0 to size -1 ) {
        pb.addTask(new Task(i, j, rand.nextInt(200)))
      }
    }

  }

  def main(args: Array[String]) {
    println("JobShop")

//    val pb1 = new planstack.constraints.stn.Problem(2, 2, 10)
//    pb1.addTask(new planstack.constraints.stn.Task(0, 0, 5))
//    pb1.addTask(new planstack.constraints.stn.Task(0, 1, 5))
//    pb1.addTask(new planstack.constraints.stn.Task(1, 0, 5))
//    pb1.addTask(new planstack.constraints.stn.Task(1, 1, 6))
    var allstn = List[AbstractSTN]()

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
    def t = System.currentTimeMillis
    val s = t
    test(10)
    println("Runtime: %d".format(t - s))

//    val stn = new planstack.constraints.stn.planstack.constraints.stn.STNIBF
//    stn.addVar(); stn.addVar(); stn.addVar(); stn.addVar();
//    stn.addConstraint(1, 2, 10)
//    stn.addConstraint(2, 0, 1)
//    stn.addConstraint(0, 3, 2)
//    stn.addConstraint(3, 2, -4)
//    stn.addConstraint(3, 1, 10)

    println("Runtime: %d".format(t - s))
  }


}