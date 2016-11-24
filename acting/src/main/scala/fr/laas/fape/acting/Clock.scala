package fr.laas.fape.acting

/**
  * Created by abitmonn on 11/23/16.
  */
object Clock {
  private var initialTime : Long = -1

  def time() : Int = {
    if(initialTime < 0)
      initialTime = System.currentTimeMillis()
    (System.currentTimeMillis() - initialTime).asInstanceOf[Int]
  }
}
