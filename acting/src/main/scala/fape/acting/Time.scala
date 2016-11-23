package fape.acting

object Time {

  private var zero = millis

  def now = ((millis - zero) / 1000).toInt

  def millis : Long = System.currentTimeMillis()
  def seconds : Long = System.currentTimeMillis() / 1000


}
