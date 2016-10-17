package fr.laas.fape.constraints.experimental

import java.io.{File, PrintWriter}
import java.util.Locale

import fr.laas.fape.constraints.stnu.morris.{PartialObservability, TemporalNetwork}

import scala.collection.mutable

object Benchmark extends App {

  val keys = List("file", "contingents", "cont-chain", "focus", "all", "runtime", "iters")

  import PartialObservability._

  val folder = "/tmp/postnu"
  val pbFile = folder + "/" + "postnu-00111.tn"

  // just run some tests first to make sure the JVM is warm before saving any result
  var pw = new PrintWriter("/dev/null")
  println("Warming up")
  for (f <- getListOfFiles(folder).take(4) if f.getName.endsWith(".tn"))
    instrumentedGetMinimalObservationSets(TemporalNetwork.loadFromFile(f.getAbsolutePath), f.getName)
  pw.close()

  println("starting")
  pw = new PrintWriter(new File("/home/abitmonn/postnu-runtime.txt"))
  pw.write(keys.mkString(" ") + "\n")
  for (f <- getListOfFiles(folder) if f.getName.endsWith(".tn")) {
    println(f.getName)
    instrumentedGetMinimalObservationSets(TemporalNetwork.loadFromFile(f.getAbsolutePath), f.getName)
  }
  pw.close()


  def time[R](block: => R) : (Long, R) = {
    val ts = System.nanoTime()
    val ret = block
    val te = System.nanoTime()
    (te-ts, ret)
  }

  var nextRecord = 0

  /** Drop in replacement for getMinimalObservationSet that will display some runtime information
    * on each invocation. */
  def instrumentedGetMinimalObservationSets(tn: TemporalNetwork, id: String) {
    import PartialObservability._
    val res = mutable.Map[String,String]()

    def runAndRecord() {
      val key = (if(useLabelsForFocus) "focused" else "naive")+"-"+(if(allSolutions) "all" else "one")
      val (runtimeNanos, iters) = time({
        PartialObservability.getMinimalObservationSets(tn)
        PartialObservability.numIterations
      })
      val runtimeMillis = runtimeNanos.toFloat/1000000
      val sb = new StringBuilder()
      sb.append(nextRecord+" ")
      nextRecord += 1
      sb.append(id+" ")
      sb.append(tn.contingents.size+" ")
      sb.append(tn.longestContingentChain+" ")
      sb.append(useLabelsForFocus+" ")
      sb.append(allSolutions+" ")
      sb.append("%1.3f".formatLocal(Locale.US, runtimeMillis)+" ")
      sb.append(iters+"\n")
      pw.write(sb.toString())
      println(sb.toString())
    }

    PartialObservability.allSolutions = true
    PartialObservability.useLabelsForFocus = true
    runAndRecord()

    PartialObservability.useLabelsForFocus = false
    runAndRecord()

    PartialObservability.allSolutions = false
    PartialObservability.useLabelsForFocus = true
    runAndRecord()

    PartialObservability.useLabelsForFocus = false
    runAndRecord()

    pw.flush()
  }

  def getListOfFiles(dir: String):List[File] = {
    val d = new File(dir)
    if (d.exists && d.isDirectory) {
        d.listFiles.filter(_.isFile).toList
    } else {
        List[File]()
    }
}
}
