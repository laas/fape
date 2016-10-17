package fr.laas.fape.gui

import java.io.FileWriter

import scala.xml.{Attribute, Node, NodeSeq}

abstract class Action(name: String, start: Int, minDur: Int, uncertainDur: Int, failedDur: Int, successDur: Int) {

  def toXml =
    <tr>
      <th>{name}</th>
      <td>{successDur}</td>
      <td>{failedDur}</td>
      <td>{uncertainDur}</td>
      <td>{minDur}</td>
      <td>{start}</td>
    </tr>

  def getStart = start
  def getName = name
}

case class Pending(name: String, start: Int, minDur: Int, uncertainDur: Int)
  extends Action(name, start, minDur, uncertainDur, 0, 0)

case class Failed(name: String, start: Int, failedDur: Int)
  extends Action(name, start, 0, 0, failedDur, 0)

case class Successful(name: String, start: Int, successDur: Int)
  extends Action(name, start, 0, 0, 0, successDur)


class SvgRect(x: Float, y:Float, width: Float, height: Float, clazz: String, attr: List[Attribute] = Nil) {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  def draw(c: Canvas): Node = {
    <rect x={c.xProj(x)} y={c.yProj(y)} width={width*c.xRatio} height={c.h} class={clazz}/>
  }
}

class SvgLine(x1:Float, y1:Float, x2:Float, y2:Float, clazz:String) {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  def draw(c: Canvas) : Node = {
    <line x1={c.xProj(x1)} y1={c.yProj(y1)} x2={c.xProj(x2)} y2={c.yProj(y2)} class={clazz}/>
  }
}

class Canvas(xOffset: Int, origWidth: Int, finalWidth: Int, yOffset: Int, numLines: Int, finalHeight: Int) {
  val xRatio : Float = finalWidth / origWidth
  val H : Float = finalHeight / (numLines + 0.5f)
  val h = H * 0.66f
  val space = H * 0.33f

  def xProj(x: Float) = xOffset + x * xRatio
  def yProj(y: Float) = yOffset +space + y * H
}

object SVG extends App {
  implicit def ts(i:Int) : String = i.toString
  implicit def ts(f:Float) : String = f.toString

  val acts = List(
    Pending("GoTo(operator2, wl2)", 1, 8, 2),
    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
    Pending("Clean(operator2, as2)", 10, 19, 3),
    Pending("Pick(PR3, glue, stock_table)", 10, 5, 2)
  )


  def getSvg(acts: Iterable[Action], time: Int) : NodeSeq = {
    getChart(acts, time).draw
  }

  def getChart(acts: Iterable[Action], time: Int) : TimedCanvas = {
    val lines = acts.map (a => {
    val label = TextLabel (a.getName, "action-name")
    a match {
    case Pending (name, start, min, uncertain) =>
      new ChartLine (label, List (
        RectElem (start, min, "pending"), RectElem (start + min + 0.1f, uncertain, "uncertain")
      ) )
    case Successful (_, start, dur) =>
      new ChartLine (label, List (RectElem (start, dur, "successful") ) )
    case Failed (_, start, dur) =>
      new ChartLine (label, List (RectElem (start, dur, "failed") ) )
  }
  })

    new TimedCanvas (lines, Some (time.toFloat) )
  }

  def print(acts: Iterable[Action], time: Int, fileName: String) {
    val f = new FileWriter(fileName)
    f.write(getSvg(acts, time).toString())
    f.close()
  }

//  def printSvgToFile(st: State, filename:String): Unit = {
//    val acts = for(a <- st.getAllActions.asScala) yield {
//      val start = st.getEarliestStartTime(a.start)
//      val earliestEnd = st.getEarliestStartTime(a.end)
//      val name = Printer.action(st, a)
//      a.status match {
//        case ActionStatus.EXECUTED => Successful(name, start, earliestEnd-start)
//        case ActionStatus.FAILED => Failed(name, start, earliestEnd-start)
//        case ActionStatus.PENDING | ActionStatus.EXECUTING =>
//          st.getDurationBounds(a) match {
//            case Some((min, max)) => Pending(name, start, min, max-min)
//            case None => Pending(name, start, earliestEnd-start, 0)
//          }
//      }
//    }
//    val filtered = acts.filterNot(a =>
//      a.getName.contains("Get") || a.getName.contains("Process") || a.getName.contains("GlueAttach"))
//
//    val sorted = filtered.sortWith((a,b) => {
//      def priority(a:Action) = {
//        var base = 0
//        if(a.getName.contains("operator2") && !a.getName.contains("HandOver")) base += 20000
//        else if(a.getName.contains("operator1") && !a.getName.contains("HandOver")) base += 55000
//        else if(a.getName.contains("PR2")) base += 30000
//        else if(a.getName.contains("PR3")) base += 10000
//        base + a.getStart
//      }
//      priority(a) < priority(b)
//    })
//    print(sorted, st.getEarliestStartTime(st.pb.earliestExecution), filename)
//  }

}

object MainSvg extends App {
  val acts = List(
    Successful("AAAAAAAAAAAa", 10, 50),
    Failed("BBBBBBBBBBBBB", 20, 15),
//    Pending("CCCCCCCCCCCC", 60, 20, 8),Pending("GoTo(operator2, wl2)", 1, 8, 2),
    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
    Pending("Clean(operator2, as2)", 10, 19, 3),
  Successful("AAAAAAAAAAAa", 10, 50),
    Failed("BBBBBBBBBBBBBBBBiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiiBBBBBBBB", 20, 15),
//    Pending("CCCCCCCCCCCC", 60, 20, 8),Pending("GoTo(operator2, wl2)", 1, 8, 2),
    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
//    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
//    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
//    Pending("Clean(operator2, as2)", 10, 19, 3),
//  Successful("AAAAAAAAAAAa", 10, 50),
//    Failed("BBBBBBBBBBBBB", 20, 15),
////    Pending("CCCCCCCCCCCC", 60, 20, 8),Pending("GoTo(operator2, wl2)", 1, 8, 2),
//    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
//    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
//    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
//    Pending("Clean(operator2, as2)", 10, 19, 3),
//  Successful("AAAAAAAAAAAa", 10, 50),
//    Failed("BBBBBBBBBBBBB", 20, 15),
////    Pending("CCCCCCCCCCCC", 60, 20, 8),Pending("GoTo(operator2, wl2)", 1, 8, 2),
//    Pending("GoTo(PR3, stock_table)", 1, 8, 2),
//    Pending("GoTo(PR2, stock_table)", 1, 8, 2),
//    //Pending("ProcessSurface(operator2, PR3, as2)", 10, 81, 0),
//    Pending("Clean(operator2, as2)", 10, 19, 3),
    Pending("Pick(PR3, glue, stock_table)", 10, 5, 2))
    .filterNot(a =>
      a.getName.contains("Get") || a.getName.contains("Process") || a.getName.contains("GlueAttach"))

  SVG.print(acts, 40, "test.svg")
}
